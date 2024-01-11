package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.PublisherIndex;

@Repository
public interface PublisherIndexRepository extends ElasticsearchRepository<PublisherIndex, String> {

    Optional<PublisherIndex> findByDatabaseId(Integer databaseId);
}
