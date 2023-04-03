package rs.teslaris.core.indexrepository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.DummyIndex;

@Repository
public interface DummyIndexRepository extends ElasticsearchRepository<DummyIndex, String> {
}
