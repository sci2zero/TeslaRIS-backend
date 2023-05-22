package rs.teslaris.core.indexrepository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.DocumentFileIndex;

@Repository
public interface DocumentFileIndexRepository
    extends ElasticsearchRepository<DocumentFileIndex, Integer> {
}
