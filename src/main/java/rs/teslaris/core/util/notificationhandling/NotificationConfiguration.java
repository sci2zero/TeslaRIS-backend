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
    }
}
