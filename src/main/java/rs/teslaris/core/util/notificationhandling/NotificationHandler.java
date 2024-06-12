package rs.teslaris.core.util.notificationhandling;

import rs.teslaris.core.model.commontypes.Notification;

public interface NotificationHandler {

    void handle(Notification notification, NotificationAction action);
}
