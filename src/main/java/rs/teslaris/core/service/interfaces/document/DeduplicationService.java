package rs.teslaris.core.service.interfaces.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.IndexType;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationSuggestion;

@Service
public interface DeduplicationService {

    Page<DeduplicationSuggestion> getDeduplicationSuggestions(Pageable pageable, IndexType type);

    boolean startDeduplicationProcessBeforeSchedule(Integer initiatingUserId);

    void deleteSuggestion(String suggestionId);

    void flagAsNotDuplicate(String suggestionId);
}
