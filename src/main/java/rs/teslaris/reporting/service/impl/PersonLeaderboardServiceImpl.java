package rs.teslaris.reporting.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.util.functional.Pair;
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
        var yearRange = constructYearRange(fromYear, toYear);
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
                    .aggregations("by_person", a -> a
                        .terms(t -> t.field("author_ids").size(10))
                    ),
                Void.class
            );

            var topPersonIds = publicationResponse.aggregations()
                .get("by_person")
                .lterms()
                .buckets()
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

            return publicationResponse.aggregations()
                .get("by_person")
                .lterms()
                .buckets()
                .array()
                .stream()
                .map(b -> {
                    Integer personId = Math.toIntExact(b.key()); // bucket key is a long
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
    public List<Pair<PersonIndex, Long>> getResearchersWithMostCitations(Integer institutionId,
                                                                         Integer fromYear,
                                                                         Integer toYear) {
        if (Objects.isNull(institutionId)) {
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
                        .must(m -> m.range(r -> r.field("databaseId").gt(JsonData.of(0))))
                    ))
                    .aggregations("by_person", a -> a
                        .terms(t -> t
                            .field("databaseId")
                            .size(10)
                        )
                        .aggregations("total_citations", sum -> sum
                            .sum(v -> v.field("total_citations"))
                        )
                    ),
                Void.class
            );
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
                double sum = bucket.aggregations()
                    .get("total_citations")
                    .sum()
                    .value();

                PersonIndex person = personMap.get((int) bucket.key());
                return Objects.nonNull(person) ? new Pair<>(person, (long) sum) : null;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare(b.b, a.b))
            .limit(10)
            .toList();
    }

    @Override
    public List<Pair<PersonIndex, Double>> getResearchersWithMostAssessmentPoints(
        Integer institutionId,
        Integer fromYear,
        Integer toYear) {
        var yearRange = constructYearRange(fromYear, toYear);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(institutionId);
        var allMergedOrganisationUnitIds = QueryUtil.getAllMergedOrganisationUnitIds(institutionId);

        var eligiblePersonIds = getEligiblePersonIds(institutionId);
        if (eligiblePersonIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            SearchResponse<Void> publicationResponse =
                elasticsearchClient.search(s -> s
                        .index("document_publication")
                        .size(0)
                        .query(q -> q
                            .bool(b -> b
                                .must(m -> m.term(t -> t.field("is_approved").value(true)))
                                .must(QueryUtil.organisationUnitMatchQuery(allMergedOrganisationUnitIds,
                                    searchFields))
                                .must(m -> m.terms(t -> t.field("assessment_points.a").terms(ts -> ts
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
                                .size(10)) // Direct terms aggregation
                            .aggregations("total_points", sum -> sum
                                .sum(v -> v.field("assessment_points.c"))
                            )
                        ),
                    Void.class
                );

            if (Objects.isNull(publicationResponse.aggregations()) ||
                Objects.isNull(publicationResponse.aggregations().get("by_person"))) {
                return Collections.emptyList();
            }

            var termsAgg = publicationResponse.aggregations().get("by_person").lterms();
            if (Objects.isNull(termsAgg)) {
                return Collections.emptyList();
            }

            var buckets = termsAgg.buckets().array();
            if (Objects.isNull(buckets) || buckets.isEmpty()) {
                return Collections.emptyList();
            }

            List<Pair<Integer, Double>> personPoints = buckets.stream()
                .map(bucket -> {
                    Integer personId = (int) bucket.key();
                    double totalPoints = bucket.aggregations().get("total_points").sum().value();
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

            return personPoints.stream()
                .map(pair -> {
                    PersonIndex person = personMap.get(pair.a);
                    return Objects.nonNull(person) ? new Pair<>(person, pair.b) : null;
                })
                .filter(Objects::nonNull)
                .toList();
        } catch (IOException e) {
            log.error("Error while fetching person assessment points leaderboard. Reason: {}",
                e.getMessage());
            return Collections.emptyList();
        }
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

    private Pair<Integer, Integer> constructYearRange(Integer startYear, Integer endYear) {
        if (Objects.isNull(startYear) || Objects.isNull(endYear)) {
            var currentYear = LocalDate.now().getYear();
            return new Pair<>(currentYear, currentYear - 10);
        }

        return new Pair<>(startYear, endYear);
    }
}
