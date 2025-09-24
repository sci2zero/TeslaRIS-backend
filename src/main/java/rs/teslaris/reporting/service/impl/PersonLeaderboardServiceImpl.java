package rs.teslaris.reporting.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import java.io.IOException;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.interfaces.PersonLeaderboardService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonLeaderboardServiceImpl implements PersonLeaderboardService {

    private final ElasticsearchClient elasticsearchClient;


    @Override
    public Map<PersonIndex, Long> getTopResearchersByPublicationCount(Integer institutionId,
                                                                      Integer fromYear,
                                                                      Integer toYear) {
        var yearRange = constructYearRange(fromYear, toYear);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyMap();
        }

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


            List<Integer> eligiblePersonIds = personIdResponse.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(PersonIndex::getDatabaseId)
                .filter(id -> id > 0)
                .toList();

            if (eligiblePersonIds.isEmpty()) {
                return Map.of();
            }

            SearchResponse<Void> publicationResponse = elasticsearchClient.search(s -> s
                    .index("document_publication")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(t -> t.field("is_approved").value(true)))
                            .must(
                                m -> m.term(t -> t.field("organisation_unit_ids").value(institutionId)))
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

            List<Integer> topPersonIds = publicationResponse.aggregations()
                .get("by_person")
                .sterms()
                .buckets()
                .array()
                .stream()
                .map(b -> Integer.parseInt(b.key().stringValue()))
                .toList();

            if (topPersonIds.isEmpty()) {
                return Map.of();
            }

            SearchResponse<PersonIndex> personResponse = elasticsearchClient.search(s -> s
                    .index("person")
                    .size(topPersonIds.size())
                    .query(q -> q
                        .terms(t -> t.field("databaseId").terms(ts -> ts
                            .value(topPersonIds.stream()
                                .map(FieldValue::of)
                                .collect(Collectors.toList()))
                        ))
                    ),
                PersonIndex.class
            );

            Map<Integer, PersonIndex> personMap = personResponse.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PersonIndex::getDatabaseId, Function.identity()));

            return publicationResponse.aggregations()
                .get("by_person")
                .sterms()
                .buckets()
                .array()
                .stream()
                .collect(Collectors.toMap(
                    b -> {
                        Integer personId = Integer.parseInt(b.key().stringValue());
                        return personMap.getOrDefault(personId, null);
                    },
                    MultiBucketBase::docCount
                ));
        } catch (IOException e) {
            log.error("Error while fetching person publication count leaderboard. Reason: {}",
                e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<PersonIndex, Long> getResearchersWithMostCitations(Integer institutionId,
                                                                  Integer fromYear,
                                                                  Integer toYear) {
        var yearRange = constructYearRange(fromYear, toYear);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyMap();
        }

        SearchResponse<PersonIndex> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("person")
                    .size(10000)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(
                                t -> t.field("employment_institutions_id_hierarchy")
                                    .value(institutionId)))
                            .must(m -> m.range(r -> r.field("databaseId").gt(JsonData.of(0))))
                        )
                    ),
                PersonIndex.class
            );
        } catch (IOException e) {
            log.error("Error while fetching person citation count leaderboard. Reason: {}",
                e.getMessage());
            return Collections.emptyMap();
        }

        return response.hits().hits().stream()
            .map(Hit::source)
            .filter(Objects::nonNull)
            .map(person -> new AbstractMap.SimpleEntry<>(person,
                Optional.ofNullable(person.getCitationsByYear())
                    .orElse(Map.of())
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getKey() >= yearRange.a && entry.getKey() <= yearRange.b)
                    .mapToLong(Map.Entry::getValue)
                    .sum()
            ))
            .filter(entry -> entry.getValue() > 0)
            .sorted(Map.Entry.<PersonIndex, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    @Override
    public Map<PersonIndex, Long> getResearchersWithMostAssessmentPoints(Integer institutionId,
                                                                         Integer fromYear,
                                                                         Integer toYear) {
        return Map.of();
    }

    private Pair<Integer, Integer> constructYearRange(Integer startYear, Integer endYear) {
        if (Objects.isNull(startYear) || Objects.isNull(endYear)) {
            var currentYear = LocalDate.now().getYear();
            return new Pair<>(currentYear, currentYear - 10);
        }

        return new Pair<>(startYear, endYear);
    }
}
