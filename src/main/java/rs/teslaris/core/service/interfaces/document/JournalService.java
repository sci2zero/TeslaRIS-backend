package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.document.Journal;

@Service
public interface JournalService {

    Journal findJournalById(Integer journalId);
}
