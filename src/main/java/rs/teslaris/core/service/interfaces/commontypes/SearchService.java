package rs.teslaris.core.service.interfaces.commontypes;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface SearchService<T> {

    Page<T> runQuery(Query query, Pageable pageable, Class<T> clazz,
                     String indexName);
}
