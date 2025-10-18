package rs.teslaris.reporting.service.impl.leaderboards;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.NamedValue;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.interfaces.leaderboards.GlobalLeaderboardService;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalLeaderboardServiceImpl implements GlobalLeaderboardService {

    private final ElasticsearchClient elasticsearchClient;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;


    @Override
    public List<Pair<PersonIndex, Long>> getPersonsWithMostCitations() {
        return getTopCitations(
            "person",
            "by_person",
            PersonIndex.class
        );
    }

    @Override
    public List<Pair<OrganisationUnitIndex, Long>> getInstitutionsWithMostCitations() {
        return getTopCitations(
            "person",
            "by_org_unit",
            organisationUnitIndexRepository::findOrganisationUnitIndexByDatabaseId
        );
    }

    @Override
    public List<Pair<DocumentPublicationIndex, Long>> getDocumentsWithMostCitations() {
        return getTopCitations(
            "document_publication",
            "by_publication",
            DocumentPublicationIndex.class
        );
    }

    private <T> List<Pair<T, Long>> getTopCitations(
        String indexName,
        String aggName,
        Class<T> clazz) {

        SearchResponse<T> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index(indexName)
                    .size(0)
                    .aggregations(aggName, a -> a
                        .terms(t -> t
                            .field("databaseId")
                            .size(5)
                            .order(List.of(
                                NamedValue.of("total_citations", SortOrder.Desc)
                            ))
                        )
                        .aggregations("total_citations",
                            sum -> sum.sum(v -> v.field("total_citations")))
                        .aggregations("top_hits",
                            th -> th.topHits(h -> h.size(1)))
                    )
                , clazz);
        } catch (IOException e) {
            log.error("Error while fetching {} citation count leaderboard. Reason: {}", indexName,
                e.getMessage());
            return Collections.emptyList();
        }

        var buckets = response.aggregations().get(aggName).lterms().buckets().array();

        return buckets.stream()
            .map(bucket -> {
                double sum = bucket.aggregations().get("total_citations").sum().value();

                if (sum <= 0) {
                    return null;
                }

                var topHits = bucket.aggregations().get("top_hits").topHits();
                var hit = topHits.hits().hits().stream().findFirst().orElse(null);

                T entity = null;
                if (Objects.nonNull(hit)) {
                    try {
                        var mapper = new ObjectMapper();
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                        var source = hit.source();
                        if (Objects.nonNull(source)) {
                            entity = mapper.convertValue(source.to(Map.class), clazz);
                        }
                    } catch (Exception e) {
                        log.error("Error converting top hit to {}", clazz.getSimpleName(), e);
                    }
                }

                return Objects.nonNull(entity) ? new Pair<>(entity, (long) sum) : null;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare(b.b, a.b))
            .limit(5)
            .toList();
    }

    private <T> List<Pair<T, Long>> getTopCitations(String indexName, String aggName,
                                                    Function<Integer, Optional<T>> lookupFn) {
        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index(indexName)
                    .size(0)
                    .aggregations(aggName, a -> a
                        .terms(t -> t.field("employment_institutions_id_hierarchy")
                            .size(5))
                        .aggregations("total_citations",
                            sum -> sum
                                .sum(v -> v.field("total_citations"))))
                , Void.class);
        } catch (IOException e) {
            log.error("Error while fetching {} citation count leaderboard. Reason: {}", indexName,
                e.getMessage());
            return Collections.emptyList();
        }

        var buckets = response
            .aggregations()
            .get(aggName)
            .lterms()
            .buckets()
            .array();

        var ids = buckets
            .stream()
            .map(LongTermsBucket::key)
            .map(Long::intValue)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        var entityMap = new HashMap<Integer, T>();
        ids.forEach(id -> lookupFn
            .apply(id)
            .ifPresent(entity -> entityMap.put(id, entity)
            )
        );

        return buckets.stream()
            .map(bucket -> {
                    double sum = bucket.aggregations().get("total_citations").sum().value();

                    if (sum <= 0) {
                        return null;
                    }

                    var entity = entityMap.get((int) bucket.key());
                    return Objects.nonNull(entity) ? new Pair<>(entity, (long) sum) : null;
                }
            ).filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare(b.b, a.b))
            .limit(5)
            .toList();
    }
}
