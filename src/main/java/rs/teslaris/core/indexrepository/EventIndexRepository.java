package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.EventIndex;

@Repository
public interface EventIndexRepository extends ElasticsearchRepository<EventIndex, String> {

    Optional<EventIndex> findByDatabaseId(Integer databaseId);

    Long countByRelatedInstitutionIds(Integer institutionId);

    Long countByClassifiedBy(Integer classifiedBy);

    Long countByRelatedInstitutionIdsAndClassifiedBy(Integer institutionId, Integer classifiedBy);
}
