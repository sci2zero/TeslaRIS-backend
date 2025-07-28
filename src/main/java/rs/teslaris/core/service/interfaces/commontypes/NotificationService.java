package rs.teslaris.core.service.interfaces.commontypes;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.NotificationActionResult;
import rs.teslaris.core.dto.commontypes.NotificationDTO;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.commontypes.NotificationType;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.core.util.notificationhandling.NotificationAction;

@Service
public interface NotificationService extends JPAService<Notification> {

    List<NotificationDTO> getUserNotifications(Integer userId);

    long getUserNotificationCount(Integer userId);

    NotificationActionResult performAction(Integer notificationId, Integer userId,
                                           NotificationAction action);

    void dismiss(Integer notificationId, Integer userId);

    void dismissAll(Integer userId);

    Notification createNotification(Notification notification);

    void cleanPastNotificationsOfType(Integer userId, NotificationType notificationType);
}
