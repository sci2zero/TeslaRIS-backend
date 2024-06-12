package rs.teslaris.core.service.impl.commontypes;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;

@Service
@RequiredArgsConstructor
public class SearchServiceImplES<T> implements SearchService<T> {

    private final ElasticsearchOperations elasticsearchTemplate;


    @Override
    public Page<T> runQuery(Query query, Pageable pageable, Class<T> clazz, String indexName) {
        var searchQueryBuilder = new NativeQueryBuilder().withQuery(query).withPageable(pageable);

        var searchQuery = searchQueryBuilder.build();

        var searchHits =
            elasticsearchTemplate.search(searchQuery, clazz, IndexCoordinates.of(indexName));

        var searchHitsPaged = SearchHitSupport.searchPageFor(searchHits, searchQuery.getPageable());

        return (Page<T>) SearchHitSupport.unwrapSearchHits(searchHitsPaged);
    }
}
