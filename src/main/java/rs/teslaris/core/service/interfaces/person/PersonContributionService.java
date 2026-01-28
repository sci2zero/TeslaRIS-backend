package rs.teslaris.core.service.interfaces.person;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.commontypes.NotificationType;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface PersonContributionService extends JPAService<PersonContribution> {

    void setPersonDocumentContributionsForDocument(Document document, DocumentDTO documentDTO);

    void setPersonPublicationSeriesContributionsForJournal(PublicationSeries publicationSeries,
                                                           PublicationSeriesDTO journalDTO);

    void setPersonPublicationSeriesContributionsForBookSeries(PublicationSeries publicationSeries,
                                                              BookSeriesDTO bookSeriesDTO);

    void setPersonEventContributionForEvent(Event event, EventDTO eventDTO);

    void deleteContribution(Integer contributionId);

    Optional<User> getUserForContributor(Integer contributorId);

    List<User> getEditorUsersForContributionInstitutionIds(Set<Integer> institutionIds);

    void notifyContributor(Notification notification, NotificationType notificationType);

    void notifyAdminsAboutUnbindedContribution(Document document);

    void reorderContributions(Set<PersonContribution> contributions,
                              Integer contributionId,
                              Integer oldContributionOrderNumber,
                              Integer newContributionOrderNumber);

    PersonDocumentContribution findContributionForResearcherAndDocument(Integer personId,
                                                                        Integer documentId);

    List<Integer> getIdsOfNonRelatedDocuments(Integer organisationUnitId, Integer personId);

    void createInvolvementsForRetroactiveExternalContributions();
}
