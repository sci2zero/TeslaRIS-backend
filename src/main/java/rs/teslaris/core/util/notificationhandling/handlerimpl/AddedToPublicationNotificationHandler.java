package rs.teslaris.core.util.notificationhandling.handlerimpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.repository.commontypes.NotificationRepository;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.util.exceptionhandling.exception.NotificationException;
import rs.teslaris.core.util.notificationhandling.NotificationAction;
import rs.teslaris.core.util.notificationhandling.NotificationHandler;

@Component
@RequiredArgsConstructor
@Transactional
public class AddedToPublicationNotificationHandler implements NotificationHandler {

    private final DocumentPublicationService documentPublicationService;

    private final NotificationRepository notificationRepository;


    @Override
    public void handle(Notification notification, NotificationAction action) {
        if (!action.equals(NotificationAction.REMOVE_FROM_PUBLICATION)) {
            throw new NotificationException("Invalid action.");
        }

        var personId = Integer.parseInt(notification.getValues().get("personId"));
        var documentId = Integer.parseInt(notification.getValues().get("documentId"));
        documentPublicationService.unbindResearcherFromContribution(personId, documentId);
    }
}
