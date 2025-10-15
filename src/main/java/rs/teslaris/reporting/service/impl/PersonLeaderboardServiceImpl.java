package rs.teslaris.reporting.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.dto.CommissionAssessmentPointsPersonLeaderboard;
import rs.teslaris.reporting.service.interfaces.PersonLeaderboardService;
import rs.teslaris.reporting.utility.QueryUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonLeaderboardServiceImpl implements PersonLeaderboardService {

    private final ElasticsearchClient elasticsearchClient;

    private final PersonIndexRepository personIndexRepository;


    @Override
    public List<Pair<PersonIndex, Long>> getTopResearchersByPublicationCount(Integer institutionId,
                                                                             Integer fromYear,
                                                                             Integer toYear) {
        var yearRange = QueryUtil.constructYearRange(fromYear, toYear);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(institutionId);
        var allMergedOrganisationUnitIds =
            QueryUtil.getAllMergedOrganisationUnitIds(institutionId);

        var eligiblePersonIds = getEligiblePersonIds(institutionId);
        if (eligiblePersonIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            SearchResponse<Void> publicationResponse = elasticsearchClient.search(s -> s
                    .index("document_publication")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(t -> t.field("is_approved").value(true)))
                            .must(QueryUtil.organisationUnitMatchQuery(allMergedOrganisationUnitIds,
                                searchFields))
                            .must(m -> m.terms(t -> t.field("author_ids").terms(ts -> ts
                                .value(eligiblePersonIds.stream()
                                    .map(FieldValue::of)
                                    .collect(Collectors.toList())))))
                            .must(m -> m.range(
                                r -> r.field("year").gte(JsonData.of(yearRange.a))
                                    .lte(JsonData.of(yearRange.b))))
                            .mustNot(m -> m.term(t -> t.field("type").value("PROCEEDINGS")))
                        )
                    )
                    .aggregations("by_all_persons", a -> a
                        .filter(f -> f
                            .terms(t -> t
                                .field("author_ids")
                                .terms(ts -> ts.value(
                                    eligiblePersonIds.stream()
                                        .map(FieldValue::of)
                                        .collect(Collectors.toList())
                                ))
                            )
                        )
                        .aggregations("by_person", aa -> aa
                            .terms(t -> t
                                .field("author_ids")
                                .size(10)
                            )
                        )
                    )
                , Void.class);

            var byAllPersonsAgg = publicationResponse.aggregations()
                .get("by_all_persons")
                .filter();

            var byPersonAgg = byAllPersonsAgg.aggregations()
                .get("by_person")
                .lterms();

            var topPersonIds = byPersonAgg.buckets()
                .array()
                .stream()
                .map(LongTermsBucket::key)
                .toList();

            if (topPersonIds.isEmpty()) {
                return Collections.emptyList();
            }

            var personMap = new HashMap<Integer, PersonIndex>();
            topPersonIds.forEach(personId -> {
                personIndexRepository.findByDatabaseId(personId.intValue()).ifPresent(index ->
                    personMap.put(personId.intValue(), index)
                );
            });

            return byPersonAgg.buckets()
                .array()
                .stream()
                .map(b -> {
                    Integer personId = Math.toIntExact(b.key());
                    PersonIndex person = personMap.get(personId);
                    return new Pair<>(person, b.docCount());
                })
                .toList();
        } catch (IOException e) {
            log.error("Error while fetching person publication count leaderboard. Reason: {}",
                e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Pair<PersonIndex, Long>> getResearchersWithMostCitations(
        Integer institutionId, Integer fromYear, Integer toYear) {

        if (Objects.isNull(institutionId)) {
            return Collections.emptyList();
        }

        var eligiblePersonIds = getEligiblePersonIds(institutionId);
        if (eligiblePersonIds.isEmpty()) {
            return Collections.emptyList();
        }

        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                .index("person")
                .size(0)
                .query(q -> q.bool(b -> b
                    .must(m -> m.term(t -> t
                        .field("employment_institutions_id_hierarchy")
                        .value(institutionId)))
                    .must(m -> m.terms(t -> t.field("databaseId").terms(ts -> ts
                        .value(eligiblePersonIds.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))))
                ))
                .aggregations("by_person", a -> {
                    var agg = a.terms(
                        t -> t
                            .field("databaseId")
                            .size(2000)
                    );

                    for (int year = fromYear; year <= toYear; year++) {
                        var currentYear = year;
                        agg.aggregations("year_" + year, sum -> sum
                            .sum(v -> v.field("citations_by_year." + currentYear)));
                    }
                    return agg;
                }), Void.class);
        } catch (IOException e) {
            log.error("Error while fetching person citation count leaderboard. Reason: {}",
                e.getMessage());
            return Collections.emptyList();
        }

        var buckets = response.aggregations()
            .get("by_person")
            .lterms()
            .buckets()
            .array();

        var topPersonIds = buckets.stream()
            .map(LongTermsBucket::key)
            .map(Long::intValue)
            .collect(Collectors.toSet());

        if (topPersonIds.isEmpty()) {
            return Collections.emptyList();
        }

        var personMap = new HashMap<Integer, PersonIndex>();
        topPersonIds.forEach(id ->
            personIndexRepository.findByDatabaseId(id).ifPresent(person ->
                personMap.put(id, person)
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
                return periodSum > 0 ? new Pair<>((int) bucket.key(), periodSum) : null;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare(b.b, a.b))
            .limit(10)
            .map(pair -> {
                PersonIndex person = personMap.get(pair.a);
                return person != null ? new Pair<>(person, pair.b) : null;
            })
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public List<CommissionAssessmentPointsPersonLeaderboard> getResearchersWithMostAssessmentPoints(
        Integer institutionId,
        Integer fromYear,
        Integer toYear) {
        var yearRange = QueryUtil.constructYearRange(fromYear, toYear);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(institutionId);
        var allMergedOrganisationUnitIds = QueryUtil.getAllMergedOrganisationUnitIds(institutionId);

        var eligiblePersonIds = getEligiblePersonIds(institutionId);
        if (eligiblePersonIds.isEmpty()) {
            return Collections.emptyList();
        }

        var commissions = QueryUtil.fetchCommissionsForOrganisationUnit(institutionId);
        var retVal = new ArrayList<CommissionAssessmentPointsPersonLeaderboard>();

        commissions.forEach(commission -> {
            try {
                SearchResponse<Void> publicationResponse =
                    elasticsearchClient.search(s -> s
                            .index("document_publication")
                            .size(0)
                            .query(q -> q
                                .bool(b -> b
                                    .must(m -> m.term(t -> t.field("is_approved").value(true)))
                                    .must(QueryUtil.organisationUnitMatchQuery(
                                        allMergedOrganisationUnitIds,
                                        searchFields))
                                    .must(
                                        m -> m.terms(t -> t.field("assessment_points.a").terms(ts -> ts
                                            .value(eligiblePersonIds.stream()
                                                .map(FieldValue::of)
                                                .collect(Collectors.toList())))))
                                    .must(m -> m.range(
                                        r -> r.field("year").gte(JsonData.of(yearRange.a))
                                            .lte(JsonData.of(yearRange.b))))
                                    .mustNot(m -> m.term(t -> t.field("type").value("PROCEEDINGS")))
                                )
                            )
                            .aggregations("by_person", a -> a
                                .terms(t -> t.field("assessment_points.a")
                                    .size(10)
                                    .order(List.of(
                                        NamedValue.of("total_points", SortOrder.Desc)
                                    )))
                                .aggregations("total_points", sum -> sum
                                    .sum(v -> v.field("assessment_points.c"))
                                )
                            ),
                        Void.class
                    );

                if (Objects.isNull(publicationResponse.aggregations()) ||
                    Objects.isNull(publicationResponse.aggregations().get("by_person"))) {
                    return;
                }

                var termsAgg = publicationResponse.aggregations().get("by_person").lterms();
                if (Objects.isNull(termsAgg)) {
                    return;
                }

                var buckets = termsAgg.buckets().array();
                if (Objects.isNull(buckets) || buckets.isEmpty()) {
                    return;
                }

                var personPoints = buckets.stream()
                    .map(bucket -> {
                        var personId = (int) bucket.key();
                        double totalPoints =
                            bucket.aggregations().get("total_points").sum().value();
                        return new Pair<>(personId, totalPoints);
                    })
                    .sorted((p1, p2) -> Double.compare(p2.b, p1.b))
                    .toList();

                var topPersonIds = personPoints.stream()
                    .map(pair -> pair.a)
                    .toList();

                var personMap = new HashMap<Integer, PersonIndex>();
                topPersonIds.forEach(personId -> {
                    personIndexRepository.findByDatabaseId(personId).ifPresent(index ->
                        personMap.put(personId, index)
                    );
                });

                var leaderboardData = personPoints.stream()
                    .map(pair -> {
                        var person = personMap.get(pair.a);
                        return Objects.nonNull(person) ? new Pair<>(person, pair.b) : null;
                    })
                    .filter(Objects::nonNull)
                    .toList();

                retVal.add(new CommissionAssessmentPointsPersonLeaderboard(commission.a,
                    MultilingualContentConverter.getMultilingualContentDTO(commission.b),
                    leaderboardData));
            } catch (IOException e) {
                log.error("Error while fetching person assessment points leaderboard. Reason: {}",
                    e.getMessage());
            }
        });

        return retVal;
    }

    private List<Integer> getEligiblePersonIds(Integer institutionId) {
        SearchResponse<PersonIndex> personIdResponse;
        try {
            personIdResponse = elasticsearchClient.search(s -> s
                    .index("person")
                    .size(10000)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(
                                t -> t.field("employment_institutions_id_hierarchy")
                                    .value(institutionId)))
                            .must(m -> m.range(r -> r.field("databaseId").gt(JsonData.of(0))))
                        )
                    )
                    .source(sc -> sc.filter(f -> f.includes("databaseId"))),
                PersonIndex.class
            );

            return personIdResponse.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(PersonIndex::getDatabaseId)
                .filter(id -> id > 0)
                .toList();
        } catch (IOException e) {
            log.error("Error while fetching eligible person IDs for institution ({}). Reason: {}",
                institutionId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
