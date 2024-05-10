package rs.teslaris.core.service.interfaces.person;

import java.util.Optional;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.model.user.User;

@Service
public interface PersonContributionService {

    void setPersonDocumentContributionsForDocument(Document document, DocumentDTO documentDTO);

    void setPersonPublicationSeriesContributionsForJournal(PublicationSeries publicationSeries,
                                                           PublicationSeriesDTO journalDTO);

    void setPersonPublicationSeriesContributionsForBookSeries(PublicationSeries publicationSeries,
                                                              BookSeriesDTO bookSeriesDTO);

    void setPersonEventContributionForEvent(Event event, EventDTO eventDTO);

    void deleteContribution(Integer contributionId);

    Optional<User> getUserForContributor(Integer contributorId);

    void notifyContributor(Notification notification);
}
