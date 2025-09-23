package rs.teslaris.core.util.notificationhandling.handlerimpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.util.exceptionhandling.exception.NotificationException;
import rs.teslaris.core.util.notificationhandling.NotificationAction;
import rs.teslaris.core.util.notificationhandling.NotificationHandler;

@Component
@RequiredArgsConstructor
@Transactional
public class EmployedResearcherUnbindedHandler implements NotificationHandler {

    private final DocumentPublicationService documentPublicationService;


    @Override
    public void handle(Notification notification, NotificationAction action) {
        if (!action.equals(NotificationAction.REMOVE_EMPLOYEES_FROM_PUBLICATION)) {
            throw new NotificationException("Invalid action.");
        }

        var institutionId = Integer.parseInt(notification.getValues().get("institutionId"));
        var documentId = Integer.parseInt(notification.getValues().get("documentId"));
        documentPublicationService.unbindInstitutionResearchersFromDocument(institutionId,
            documentId);
    }
}
