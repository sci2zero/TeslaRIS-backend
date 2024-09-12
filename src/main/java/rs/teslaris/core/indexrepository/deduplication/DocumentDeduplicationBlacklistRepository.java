package rs.teslaris.core.indexrepository.deduplication;

import java.util.Optional;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationBlacklist;

@Repository
public interface DocumentDeduplicationBlacklistRepository
    extends ElasticsearchRepository<DeduplicationBlacklist, String> {

    @Query("{\"bool\": " +
        "{ \"should\": [ " +
        "{ \"bool\": " +
        "{ \"must\": [ " +
        "{ \"term\": { \"left_entity_database_id\": \"?0\" } }, " +
        "{ \"term\": { \"right_entity_database_id\": \"?1\" } }, " +
        "{ \"term\": { \"entity_type\": \"?2\" } } ] } }, " +
        "{ \"bool\": " +
        "{ \"must\": [ " +
        "{ \"term\": { \"left_entity_database_id\": \"?1\" } }, " +
        "{ \"term\": { \"right_entity_database_id\": \"?0\" } }, " +
        "{ \"term\": { \"entity_type\": \"?2\" } } " +
        "] } } ] } }")
    Optional<DeduplicationBlacklist> findByEntityIdsAndEntityType(Integer leftId, Integer rightId,
                                                                  String entityType);
}
