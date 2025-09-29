package rs.teslaris.reporting.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TermsInclude;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.dto.CommissionAssessmentPointsOULeaderboard;
import rs.teslaris.reporting.service.interfaces.OrganisationUnitLeaderboardService;
import rs.teslaris.reporting.utility.QueryUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganisationUnitLeaderboardServiceImpl implements OrganisationUnitLeaderboardService {

    private final ElasticsearchClient elasticsearchClient;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;


    @Override
    public List<Pair<OrganisationUnitIndex, Long>> getTopSubUnitsByPublicationCount(
        Integer institutionId, Integer fromYear, Integer toYear) {
        var yearRange = QueryUtil.constructYearRange(fromYear, toYear);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(institutionId);

        var eligibleOUIds = getEligibleOrganisationUnitIds(institutionId);
        if (eligibleOUIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            SearchResponse<Void> publicationResponse = elasticsearchClient.search(s -> s
                    .index("document_publication")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(t -> t.field("is_approved").value(true)))
                            .must(QueryUtil.organisationUnitMatchQuery(eligibleOUIds,
                                searchFields))
                            .must(m -> m.range(
                                r -> r.field("year").gte(JsonData.of(yearRange.a))
                                    .lte(JsonData.of(yearRange.b))))
                            .mustNot(m -> m.term(t -> t.field("type").value("PROCEEDINGS")))
                        )
                    )
                    .aggregations("by_org_unit", a -> a.terms(
                        t -> t
                            .field("organisation_unit_ids")
                            .size(10)
                            .include(TermsInclude.of(
                                i -> i.terms(eligibleOUIds
                                    .stream()
                                    .map(Object::toString)
                                    .toList()
                                ))))
                    ),
                Void.class
            );

            var topOUIds = publicationResponse.aggregations()
                .get("by_org_unit")
                .lterms()
                .buckets()
                .array()
                .stream()
                .map(LongTermsBucket::key)
                .toList();

            if (topOUIds.isEmpty()) {
                return Collections.emptyList();
            }

            var organisationUnitMap = new HashMap<Integer, OrganisationUnitIndex>();
            topOUIds.forEach(organisationUnitId -> {
                organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
                    organisationUnitId.intValue()).ifPresent(index ->
                    organisationUnitMap.put(organisationUnitId.intValue(), index)
                );
            });

            return publicationResponse.aggregations()
                .get("by_org_unit")
                .lterms()
                .buckets()
                .array()
                .stream()
                .map(b -> {
                    var organisationUnitId = Math.toIntExact(b.key());
                    var organisationUnit = organisationUnitMap.get(organisationUnitId);
                    return new Pair<>(organisationUnit, b.docCount());
                })
                .toList();
        } catch (IOException e) {
            log.error("Error while fetching OU publication count leaderboard. Reason: {}",
                e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Pair<OrganisationUnitIndex, Long>> getSubUnitsWithMostCitations(
        Integer institutionId, Integer fromYear, Integer toYear) {

        if (Objects.isNull(institutionId)) {
            return Collections.emptyList();
        }

        var eligibleOUIds = getEligibleOrganisationUnitIds(institutionId);
        if (eligibleOUIds.isEmpty()) {
            return Collections.emptyList();
        }

        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                .index("person")
                .size(0)
                .query(q -> q.bool(b -> b
                    .must(m -> m.terms(t ->
                        t.field("employment_institutions_id_hierarchy").terms(
                            v -> v.value(eligibleOUIds.stream()
                                .map(FieldValue::of)
                                .toList())
                        )))
                    .must(m -> m.range(r -> r.field("databaseId").gt(JsonData.of(0))))
                ))
                .aggregations("by_org_unit", a -> {
                    var agg = a.terms(
                        t -> t
                            .field("employment_institutions_id_hierarchy")
                            .size(10)
                            .include(TermsInclude.of(
                                i -> i.terms(eligibleOUIds.stream()
                                    .map(Object::toString)
                                    .toList()
                                ))));

                    for (int year = fromYear; year <= toYear; year++) {
                        var finalYear = year;
                        agg.aggregations("year_" + year, sum -> sum
                            .sum(v -> v.field("citations_by_year." + finalYear)));
                    }
                    return agg;
                }), Void.class);
        } catch (IOException e) {
            log.error("Error while fetching OU citation count leaderboard. Reason: {}",
                e.getMessage());
            return Collections.emptyList();
        }

        var buckets = response.aggregations()
            .get("by_org_unit")
            .lterms()
            .buckets()
            .array();

        var topOUIds = buckets.stream()
            .map(LongTermsBucket::key)
            .map(Long::intValue)
            .collect(Collectors.toSet());

        if (topOUIds.isEmpty()) {
            return Collections.emptyList();
        }

        var organisationUnitMap = new HashMap<Integer, OrganisationUnitIndex>();
        topOUIds.forEach(id ->
            organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(id)
                .ifPresent(organisationUnit ->
                    organisationUnitMap.put(id, organisationUnit)
                )
        );

        return buckets.stream()
            .map(bucket -> {
                long periodSum = 0L;
                for (int year = fromYear; year <= toYear; year++) {
                    var agg = bucket.aggregations().get("year_" + year).sum();
                    if (agg != null && agg.value() > 0) {
                        periodSum += (long) agg.value();
                    }
                }

                var ou = organisationUnitMap.get((int) bucket.key());
                return (periodSum > 0 && ou != null) ? new Pair<>(ou, periodSum) : null;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare(b.b, a.b))
            .limit(10)
            .toList();
    }

    @Override
    public List<CommissionAssessmentPointsOULeaderboard> getSubUnitsWithMostAssessmentPoints(
        Integer institutionId, Integer fromYear, Integer toYear) {
        var yearRange = QueryUtil.constructYearRange(fromYear, toYear);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(institutionId);

        var eligibleOUIds = getEligibleOrganisationUnitIds(institutionId);
        if (eligibleOUIds.isEmpty()) {
            return Collections.emptyList();
        }

        var commissions =
            new HashSet<>(QueryUtil.fetchCommissionsForOrganisationUnit(institutionId));
        eligibleOUIds.forEach(organisationUnitId -> commissions.addAll(
            QueryUtil.fetchCommissionsForOrganisationUnit(organisationUnitId)));

        var retVal = new ArrayList<CommissionAssessmentPointsOULeaderboard>();

        commissions.forEach(commission -> {
            try {
                SearchResponse<Void> publicationResponse =
                    elasticsearchClient.search(s -> s
                            .index("document_publication")
                            .size(0)
                            .query(q -> q
                                .bool(b -> b
                                    .must(m -> m.term(t -> t.field("is_approved").value(true)))
                                    .must(QueryUtil.organisationUnitMatchQuery(eligibleOUIds,
                                        searchFields))
                                    .must(m -> m.term(
                                        t -> t.field("assessment_points.b").value(commission.a)))
                                    .must(m -> m.range(
                                        r -> r.field("year").gte(JsonData.of(yearRange.a))
                                            .lte(JsonData.of(yearRange.b))))
                                    .mustNot(m -> m.term(t -> t.field("type").value("PROCEEDINGS")))
                                )
                            )
                            .aggregations("by_org_unit", a -> a.terms(
                                    t -> t
                                        .field("organisation_unit_ids")
                                        .size(10)
                                        .include(TermsInclude.of(
                                            i -> i.terms(eligibleOUIds
                                                .stream()
                                                .map(Object::toString)
                                                .toList()
                                            ))))
                                .aggregations("total_points", sum -> sum
                                    .sum(v -> v.field("assessment_points.c"))
                                )
                            ),
                        Void.class
                    );

                if (Objects.isNull(publicationResponse.aggregations()) ||
                    Objects.isNull(publicationResponse.aggregations().get("by_org_unit"))) {
                    return;
                }

                var termsAgg = publicationResponse.aggregations().get("by_org_unit").lterms();
                if (Objects.isNull(termsAgg)) {
                    return;
                }

                var buckets = termsAgg.buckets().array();
                if (Objects.isNull(buckets) || buckets.isEmpty()) {
                    return;
                }

                var ouPoints = buckets.stream()
                    .map(bucket -> {
                        var organisationUnitId = (int) bucket.key();
                        double totalPoints =
                            bucket.aggregations().get("total_points").sum().value();
                        return new Pair<>(organisationUnitId, totalPoints);
                    })
                    .sorted((p1, p2) -> Double.compare(p2.b, p1.b))
                    .toList();

                var topOUIds = ouPoints.stream()
                    .map(pair -> pair.a)
                    .toList();

                var organisationUnitMap = new HashMap<Integer, OrganisationUnitIndex>();
                topOUIds.forEach(organisationUnitId -> {
                    organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
                        organisationUnitId).ifPresent(index ->
                        organisationUnitMap.put(organisationUnitId, index)
                    );
                });

                var leaderboardData = ouPoints.stream()
                    .map(pair -> {
                        OrganisationUnitIndex organisationUnit = organisationUnitMap.get(pair.a);
                        return Objects.nonNull(organisationUnit) ?
                            new Pair<>(organisationUnit, pair.b) : null;
                    })
                    .filter(Objects::nonNull)
                    .toList();

                retVal.add(new CommissionAssessmentPointsOULeaderboard(commission.a,
                    MultilingualContentConverter.getMultilingualContentDTO(commission.b),
                    leaderboardData));
            } catch (IOException e) {
                log.error("Error while fetching OU assessment points leaderboard. Reason: {}",
                    e.getMessage());
            }
        });

        return retVal;
    }

    private List<Integer> getEligibleOrganisationUnitIds(Integer institutionId) {
        SearchResponse<OrganisationUnitIndex> ouIdResponse;
        try {
            ouIdResponse = elasticsearchClient.search(s -> s
                    .index("organisation_unit")
                    .size(10000)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(
                                t -> t.field("super_ou_id").value(institutionId)))
                            .must(m -> m.range(r -> r.field("databaseId").gt(JsonData.of(0))))
                        )
                    )
                    .source(sc -> sc.filter(f -> f.includes("databaseId"))),
                OrganisationUnitIndex.class
            );

            return ouIdResponse.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(OrganisationUnitIndex::getDatabaseId)
                .filter(id -> id > 0)
                .toList();
        } catch (IOException e) {
            log.error("Error while fetching eligible OU IDs for institution ({}). Reason: {}",
                institutionId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
