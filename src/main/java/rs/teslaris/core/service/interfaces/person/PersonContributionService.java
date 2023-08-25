package rs.teslaris.core.service.interfaces.person;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.document.Journal;

@Service
public interface PersonContributionService {

    void setPersonDocumentContributionsForDocument(Document document, DocumentDTO documentDTO);

    void setPersonJournalContributionsForJournal(Journal journal, JournalDTO journalDTO);

    void setPersonEventContributionForEvent(Event event, EventDTO eventDTO);
}
