package rs.teslaris.core.service.impl.commontypes;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.util.functional.Pair;

@Service
@RequiredArgsConstructor
@Traceable
@Slf4j
public class SearchServiceImplES<T> implements SearchService<T> {

    private final ElasticsearchOperations elasticsearchTemplate;

    private final ElasticsearchClient elasticsearchClient;

    // Small-record indexes that do not need to implement field omission logic
    private final List<String> indexesExcludedFromFieldOmission = List.of("prize");

    // Very large fields, not needed for display, only for search
    private final List<String> fieldsToOmit = List.of(
        "full_text_sr", "full_text_other", "description_sr", "description_other"
    );


    @Override
    public Page<T> runQuery(Query query, Pageable pageable, Class<T> clazz, String indexName) {
        int targetPage = pageable.isUnpaged() ? -1 : pageable.getPageNumber();

        // For first 100 records, use regular pagination (it's faster for small offsets)
        if (targetPage < 10) {
            return runRegularQuery(query, pageable, clazz, indexName);
        }

        // For deep pages, use search_after with geometric sequential fetching
        return runSearchAfterSequential(query, pageable, clazz, indexName);
    }

    private Page<T> runRegularQuery(Query query, Pageable pageable, Class<T> clazz,
                                    String indexName) {
        var searchQueryBuilder = new NativeQueryBuilder()
            .withQuery(query)
            .withPageable(pageable)
            .withTrackTotalHits(true)
            .withSourceFilter(new FetchSourceFilterBuilder()
                .withExcludes(!indexesExcludedFromFieldOmission.contains(indexName) ?
                    fieldsToOmit.toArray(new String[0]) : new String[] {})
                .build());

        var searchQuery = searchQueryBuilder.build();
        var searchHits =
            elasticsearchTemplate.search(searchQuery, clazz, IndexCoordinates.of(indexName));
        var searchHitsPaged = SearchHitSupport.searchPageFor(searchHits, searchQuery.getPageable());

        return (Page<T>) SearchHitSupport.unwrapSearchHits(searchHitsPaged);
    }

    private Page<T> runSearchAfterSequential(Query query, Pageable pageable, Class<T> clazz,
                                             String indexName) {
        int targetPage = pageable.getPageNumber() + 1;
        int uiPageSize = pageable.getPageSize();
        int targetOffset = targetPage * uiPageSize;

        var stableSort = buildStableSort(pageable.getSort());

        List<Object> searchAfter = null;
        SearchHits<T> currentPageHits = null;

        int processed = 0;
        while (processed <= targetOffset) {
            int remaining = targetOffset - processed;

            int pageSize;
            if (remaining > 10000) { // cap at 10k, default ES fetch limit
                pageSize = 10000;
            } else if (remaining > 1000) {
                pageSize = 1000;
            } else if (remaining > 100) {
                pageSize = 100;
            } else {
                pageSize = uiPageSize;
            }

            var searchQueryBuilder = new NativeQueryBuilder()
                .withQuery(query)
                .withSort(stableSort)
                .withPageable(PageRequest.of(0, pageSize))
                .withTrackTotalHits(true)
                .withSourceFilter(new FetchSourceFilterBuilder()
                    .withExcludes(!indexesExcludedFromFieldOmission.contains(indexName) ?
                        fieldsToOmit.toArray(new String[0]) : new String[] {})
                    .build());

            if (Objects.nonNull(searchAfter)) {
                searchQueryBuilder.withSearchAfter(searchAfter);
            }

            var searchQuery = searchQueryBuilder.build();
            currentPageHits =
                elasticsearchTemplate.search(searchQuery, clazz, IndexCoordinates.of(indexName));
            var currentPageHitsCount = currentPageHits.getSearchHits().size();

            if (!currentPageHits.hasSearchHits() || currentPageHits.getSearchHits().isEmpty()) {
                break;
            }

            processed += currentPageHitsCount;

            var lastHit = currentPageHits.getSearchHits().getLast();
            searchAfter = lastHit.getSortValues();

            if (processed >= targetOffset || currentPageHitsCount < pageSize) {
                break;
            }
        }

        return convertSearchHitsToPage(currentPageHits, pageable);
    }

    private Sort buildStableSort(Sort originalSort) {
        List<Sort.Order> orders = new ArrayList<>();

        if (Objects.nonNull(originalSort) && originalSort.isSorted()) {
            originalSort.forEach(orders::add);
        } else {
            // if unsorted search has been performed
            orders.add(Sort.Order.desc("_score"));
        }

        boolean hasDocSort = orders.stream()
            .anyMatch(order -> "_doc".equals(order.getProperty()));

        // final tie-breaker, when listing all
        if (!hasDocSort) {
            orders.add(Sort.Order.asc("_doc"));
        }

        return Sort.by(orders);
    }

    private Page<T> convertSearchHitsToPage(SearchHits<T> searchHits, Pageable originalPageable) {
        if (Objects.isNull(searchHits) || !searchHits.hasSearchHits()) {
            return new PageImpl<>(Collections.emptyList(), originalPageable, 0);
        }

        List<T> content = searchHits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());

        long totalElements = searchHits.getTotalHits();

        return new PageImpl<>(content, originalPageable, totalElements);
    }

    @Override
    public List<Pair<String, Long>> runWordCloudSearch(Query query, String indexName,
                                                       boolean foreignLanguage) {
        SearchResponse<Void> response;
        var wordCloud = new ArrayList<Pair<String, Long>>();

        try {
            response = elasticsearchClient.search(b -> b
                    .index(indexName)
                    .size(0)
                    .query(query)
                    .aggregations("wordcloud", a -> a
                        .terms(h -> h
                            .field("wordcloud_tokens_" + (foreignLanguage ? "other" : "sr"))
                            .size(100)
                        )
                    ),
                Void.class
            );
        } catch (IOException e) {
            return wordCloud;
        }

        response.aggregations()
            .get("wordcloud")
            .sterms()
            .buckets().array().forEach(bucket -> {
                wordCloud.add(new Pair<>(bucket.key().stringValue(), bucket.docCount()));
            });

        return wordCloud;
    }

    @Override
    public Long count(Query query, String indexName) {
        try {
            var countResponse = elasticsearchClient.count(c -> c
                .index(indexName)
                .query(query)
            );

            return countResponse.count();

        } catch (IOException e) {
            log.error("Failed to count documents in index {}. Reason: {}", indexName,
                e.getMessage());
            return 0L;
        }
    }
}
