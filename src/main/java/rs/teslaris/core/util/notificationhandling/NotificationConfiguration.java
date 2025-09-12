package rs.teslaris.core.util.notificationhandling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rs.teslaris.core.model.commontypes.NotificationType;

public class NotificationConfiguration {

    public static Map<NotificationType, List<NotificationAction>> allowedActions = new HashMap<>();

    // Static initializer block to populate the map
    static {
        allowedActions.put(NotificationType.NEW_OTHER_NAME_DETECTED,
            List.of(NotificationAction.APPROVE));
        allowedActions.put(NotificationType.NEW_PAPER_HARVESTED, List.of());
        allowedActions.put(NotificationType.ADDED_TO_PUBLICATION,
            List.of(NotificationAction.REMOVE_FROM_PUBLICATION));
        allowedActions.put(NotificationType.DEDUPLICATION_SCAN_FINISHED,
            List.of(NotificationAction.PERFORM_DEDUPLICATION));
        allowedActions.put(NotificationType.FOUND_POTENTIAL_CLAIMS,
            List.of(NotificationAction.BROWSE_CLAIMABLE_DOCUMENTS));
        allowedActions.put(NotificationType.SCHEDULED_TASK_COMPLETED, List.of());
        allowedActions.put(NotificationType.NEW_EVENTS_TO_CLASSIFY,
            List.of(NotificationAction.PERFORM_EVENT_CLASSIFICATION));
        allowedActions.put(NotificationType.NEW_PUBLICATIONS_TO_ASSESS,
            List.of(NotificationAction.PERFORM_DOCUMENT_ASSESSMENT));
        allowedActions.put(NotificationType.PROMOTION_NOTIFICATION,
            List.of(NotificationAction.GO_TO_PROMOTIONS_PAGE));
        allowedActions.put(NotificationType.NEW_IMPORTS_HARVESTED,
            List.of(NotificationAction.GO_TO_HARVESTER_PAGE));
        allowedActions.put(NotificationType.NEW_ENTITY_CREATION,
            List.of(NotificationAction.NAVIGATE_TO_URL));
        allowedActions.put(NotificationType.NEW_DOCUMENTS_FOR_VALIDATION,
            List.of(NotificationAction.GO_TO_VALIDATION_PAGE));
        allowedActions.put(NotificationType.NEW_AUTHOR_UNBINDING,
            List.of(NotificationAction.REMOVE_FROM_PUBLICATION));
        allowedActions.put(NotificationType.ALL_AUTHORS_UNBINDED,
            List.of(NotificationAction.GO_TO_UNBINDED_PUBLICATIONS_PAGE));
        allowedActions.put(NotificationType.NEW_EMPLOYED_RESEARCHER_UNBINDED,
            List.of(NotificationAction.REMOVE_EMPLOYEES_FROM_PUBLICATION));
    }
}
