package rs.teslaris.core.service.interfaces.person;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.document.PublicationSeries;

@Service
public interface PersonContributionService {

    void setPersonDocumentContributionsForDocument(Document document, DocumentDTO documentDTO);

    void setPersonPublicationSeriesContributionsForJournal(PublicationSeries publicationSeries,
                                                           JournalDTO journalDTO);

    void setPersonPublicationSeriesContributionsForBookSeries(PublicationSeries publicationSeries,
                                                              BookSeriesDTO bookSeriesDTO);

    void setPersonEventContributionForEvent(Event event, EventDTO eventDTO);
}
