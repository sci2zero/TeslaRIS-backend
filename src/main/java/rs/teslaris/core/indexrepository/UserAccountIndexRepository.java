package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.UserAccountIndex;

@Repository
public interface UserAccountIndexRepository
    extends ElasticsearchRepository<UserAccountIndex, String> {

    Optional<UserAccountIndex> findByDatabaseId(Integer databaseId);
}
