package rs.teslaris.core.indexrepository.deduplication;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationSuggestion;

@Repository
public interface DocumentDeduplicationSuggestionRepository
    extends ElasticsearchRepository<DeduplicationSuggestion, String> {

    Page<DeduplicationSuggestion> findByEntityType(String entityType, Pageable pageable);

    @Query("{\"bool\": " +
        "{ \"should\": [ " +
        "{ \"bool\": " +
        "{ \"must\": [ " +
        "{ \"term\": { \"left_entity_database_id\": \"?0\" } }, " +
        "{ \"term\": { \"entity_type\": \"?1\" } } ] } }, " +
        "{ \"bool\": " +
        "{ \"must\": [ " +
        "{ \"term\": { \"right_entity_database_id\": \"?0\" } }, " +
        "{ \"term\": { \"entity_type\": \"?1\" } } " +
        "] } } ] } }")
    List<DeduplicationSuggestion> findByEntityIdAndEntityType(Integer entityId, String entityType);

    void deleteByEntityType(String entityType);
}
