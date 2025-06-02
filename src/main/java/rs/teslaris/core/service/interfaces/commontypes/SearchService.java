package rs.teslaris.core.service.interfaces.commontypes;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.util.Pair;

@Service
public interface SearchService<T> {

    Page<T> runQuery(Query query, Pageable pageable, Class<T> clazz,
                     String indexName);

    List<Pair<String, Long>> runWordCloudSearch(Query query, String indexName,
                                                boolean foreignLanguage);
}
