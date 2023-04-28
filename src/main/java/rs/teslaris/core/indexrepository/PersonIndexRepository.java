package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.PersonIndex;

@Repository
public interface PersonIndexRepository extends ElasticsearchRepository<PersonIndex, String> {

    Optional<PersonIndex> findByDatabaseId(Integer databaseId);
}
