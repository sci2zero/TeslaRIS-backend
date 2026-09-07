package rs.teslaris.core.service.interfaces.commontypes;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.util.functional.Pair;

@Service
public interface SearchService<T> {

    Page<T> runQuery(Query query, Pageable pageable, Class<T> clazz,
                     String indexName);

    /**
     * Runs the query without asking Elasticsearch for the exact total. A scan that pages through a
     * large match set only ever reads the hits, and an exact count is a full pass over every
     * matching document on every batch.
     */
    Page<T> runQueryWithoutTotal(Query query, Pageable pageable, Class<T> clazz,
                                 String indexName);

    List<Pair<String, Long>> runWordCloudSearch(Query query, String indexName,
                                                boolean foreignLanguage);

    Long count(Query query, String indexName);
}
