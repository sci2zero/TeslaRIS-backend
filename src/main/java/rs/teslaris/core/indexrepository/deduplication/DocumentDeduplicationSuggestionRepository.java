package rs.teslaris.core.indexrepository.deduplication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationSuggestion;

@Repository
public interface DocumentDeduplicationSuggestionRepository
    extends ElasticsearchRepository<DeduplicationSuggestion, String> {

    Page<DeduplicationSuggestion> findByEntityType(String entityType, Pageable pageable);

    void deleteByEntityType(String entityType);
}
