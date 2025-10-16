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
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.interfaces.DocumentLeaderboardService;
import rs.teslaris.reporting.utility.QueryUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentLeaderboardServiceImpl implements DocumentLeaderboardService {

    private final ElasticsearchClient elasticsearchClient;

    private final OrganisationUnitService organisationUnitService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;


    @Override
    public List<Pair<DocumentPublicationIndex, Long>> getPublicationsWithMostCitations(
        Integer institutionId, Integer fromYear, Integer toYear) {
        var yearRange = QueryUtil.constructYearRange(fromYear, toYear);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(institutionId);

        var eligibleOUIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId);

        try {
            SearchResponse<DocumentPublicationIndex> publicationResponse =
                elasticsearchClient.search(s -> s
                        .index("document_publication")
                        .size(10)
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
                        .aggregations("top_documents", a -> a.terms(
                                t -> t
                                    .field("databaseId")
                                    .size(10)
                                    .order(List.of(
                                        NamedValue.of("by_citation_count", SortOrder.Desc)
                                    )))
                            .aggregations("by_citation_count", sum -> sum
                                .sum(v -> v.field("total_citations"))
                            ))
                        .source(sc -> sc.filter(f -> f.includes("databaseId", "apa", "type"))),
                    DocumentPublicationIndex.class
                );

            var citationCountsByDatabaseId = new HashMap<Integer, Long>();
            if (publicationResponse.aggregations() != null &&
                publicationResponse.aggregations().get("top_documents") != null) {

                var topDocumentsAgg =
                    publicationResponse.aggregations().get("top_documents").lterms();
                if (topDocumentsAgg != null && topDocumentsAgg.buckets() != null) {
                    for (var bucket : topDocumentsAgg.buckets().array()) {
                        var databaseId = (int) bucket.key();
                        Long citationCount =
                            (long) bucket.aggregations().get("by_citation_count").sum().value();
                        citationCountsByDatabaseId.put(databaseId, citationCount);
                    }
                }
            }

            return publicationResponse.hits().hits().stream()
                .map(hit -> {
                    var publication = hit.source();
                    var databaseId = publication != null ? publication.getDatabaseId() : null;
                    Long citationCount = citationCountsByDatabaseId.getOrDefault(databaseId, 0L);
                    return new Pair<>(publication, citationCount);
                })
                .filter(hit -> Objects.nonNull(hit.a) && hit.b > 0)
                .sorted((a, b) -> Long.compare(b.b, a.b))
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error while fetching Document publication count leaderboard. Reason: {}",
                e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Pair<DocumentPublicationIndex, Long>> getTopPublicationsByStatisticCount(
        Integer institutionId,
        StatisticsType statisticsType,
        LocalDate from,
        LocalDate to) {
        if (Objects.isNull(institutionId)) {
            return Collections.emptyList();
        }

        var eligibleDocumentIds =
            getEligibleDocumentIds(institutionId);
        if (eligibleDocumentIds.isEmpty()) {
            return Collections.emptyList();
        }

        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("type").value(statisticsType.name())))
                        .must(m -> m.term(t -> t.field("is_bot").value(false)))
                        .must(m -> m.terms(t -> t.field("document_id").terms(ts -> ts
                            .value(eligibleDocumentIds.stream().map(FieldValue::of).toList()))))
                        .must(m -> m.range(r -> r
                            .field("timestamp")
                            .gte(JsonData.of(from))
                            .lte(JsonData.of(to))
                        ))
                    ))
                    .aggregations("by_document", a -> a.terms(t -> t
                                .field("document_id")
                                .size(10)
                                .order(List.of(NamedValue.of("stat_count", SortOrder.Desc)))
                            )
                            .aggregations("stat_count", sub -> sub.valueCount(v -> v.field("document_id")))
                    ),
                Void.class
            );
        } catch (IOException e) {
            log.error("Error fetching top documents by {} count: {}", statisticsType.name(),
                e.getMessage());
            return Collections.emptyList();
        }

        var buckets = response.aggregations()
            .get("by_document")
            .lterms()
            .buckets()
            .array();

        var topDocumentIds = buckets.stream()
            .map(LongTermsBucket::key)
            .map(Long::intValue)
            .collect(Collectors.toSet());

        var documentMap = new HashMap<Integer, DocumentPublicationIndex>();
        topDocumentIds.forEach(id ->
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(id)
                .ifPresent(p -> documentMap.put(id, p))
        );

        return buckets.stream()
            .map(bucket -> {
                var viewCountAgg = bucket.aggregations().get("stat_count").valueCount();
                long viewCount = Objects.isNull(viewCountAgg) || Double.isNaN(viewCountAgg.value())
                    ? 0L
                    : (long) viewCountAgg.value();
                var document = documentMap.get((int) bucket.key());
                return Objects.nonNull(document) ? new Pair<>(document, viewCount) : null;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare(b.b, a.b))
            .limit(10)
            .toList();
    }

    private List<Integer> getEligibleDocumentIds(Integer institutionId) {
        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(institutionId);
        var allMergedOrganisationUnitIds = QueryUtil.getAllMergedOrganisationUnitIds(institutionId);

        SearchResponse<DocumentPublicationIndex> documentIdResponse;
        try {
            documentIdResponse = elasticsearchClient.search(s -> s
                    .index("document_publication")
                    .size(50000)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(t -> t.field("is_approved").value(true)))
                            .must(QueryUtil.organisationUnitMatchQuery(
                                allMergedOrganisationUnitIds,
                                searchFields))
                            .must(m -> m.range(r -> r.field("databaseId").gt(JsonData.of(0))))
                        )
                    )
                    .source(sc -> sc.filter(f -> f.includes("databaseId"))),
                DocumentPublicationIndex.class
            );

            return documentIdResponse.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(DocumentPublicationIndex::getDatabaseId)
                .filter(id -> id > 0)
                .toList();
        } catch (IOException e) {
            log.error("Error while fetching eligible document IDs for institution ({}). Reason: {}",
                institutionId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
