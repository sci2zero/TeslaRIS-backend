package rs.teslaris.core.service.impl.commontypes;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.util.Pair;

@Service
@RequiredArgsConstructor
@Traceable
public class SearchServiceImplES<T> implements SearchService<T> {

    private final ElasticsearchOperations elasticsearchTemplate;

    private final ElasticsearchClient elasticsearchClient;


    @Override
    public Page<T> runQuery(Query query, Pageable pageable, Class<T> clazz, String indexName) {
        var searchQueryBuilder = new NativeQueryBuilder()
            .withQuery(query)
            .withPageable(pageable)
            .withTrackTotalHits(true);

        var searchQuery = searchQueryBuilder.build();

        var searchHits =
            elasticsearchTemplate.search(searchQuery, clazz, IndexCoordinates.of(indexName));

        var searchHitsPaged = SearchHitSupport.searchPageFor(searchHits, searchQuery.getPageable());

        return (Page<T>) SearchHitSupport.unwrapSearchHits(searchHitsPaged);
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
}
