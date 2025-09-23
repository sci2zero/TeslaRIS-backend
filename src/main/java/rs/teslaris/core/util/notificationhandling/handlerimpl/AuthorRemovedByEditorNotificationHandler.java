package rs.teslaris.core.util.notificationhandling.handlerimpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.repository.document.PersonContributionRepository;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotificationException;
import rs.teslaris.core.util.notificationhandling.NotificationAction;
import rs.teslaris.core.util.notificationhandling.NotificationHandler;

@Component
@RequiredArgsConstructor
@Transactional
public class AuthorRemovedByEditorNotificationHandler implements NotificationHandler {

    private final PersonContributionRepository contributionRepository;

    private final DocumentPublicationService documentPublicationService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final PersonService personService;


    @Override
    public void handle(Notification notification, NotificationAction action) {
        if (!action.equals(NotificationAction.RETURN_TO_PUBLICATION)) {
            throw new NotificationException("Invalid action.");
        }

        var personId = Integer.parseInt(notification.getValues().get("personId"));
        var documentId = Integer.parseInt(notification.getValues().get("documentId"));
        var contributionId = Integer.parseInt(notification.getValues().get("contributionId"));
        contributionRepository.findById(contributionId).ifPresent(contribution -> {
            contribution.setPerson(personService.findOne(personId));

            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
                .ifPresent(index -> {
                    documentPublicationService.indexCommonFields(
                        documentPublicationService.findOne(documentId), index);
                    documentPublicationIndexRepository.save(index);
                });

            contributionRepository.save(contribution);
        });
    }
}
