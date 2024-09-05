package rs.teslaris.core.service.interfaces.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.DocumentDeduplicationSuggestionDTO;

@Service
public interface DeduplicationService {

    Page<DocumentDeduplicationSuggestionDTO> getDeduplicationSuggestions(Pageable pageable);

    boolean startDocumentDeduplicationProcessBeforeSchedule();

    void deleteDocumentSuggestion(Integer suggestionId);

    void flagDocumentAsNotDuplicate(Integer suggestionId);
}
