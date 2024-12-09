package rs.teslaris.core.service.interfaces.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationSuggestion;

@Service
public interface DeduplicationService {

    Page<DeduplicationSuggestion> getDeduplicationSuggestions(Pageable pageable, EntityType type);

    boolean canPerformDeduplication();

    void startDeduplicationAsync(Integer initiatingUserId);

    void deleteSuggestion(String suggestionId);

    void deleteSuggestion(Integer deletedEntityId, EntityType entityType);

    void flagAsNotDuplicate(String suggestionId);
}
