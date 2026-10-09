package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.NotificationActionResult;
import rs.teslaris.core.dto.commontypes.NotificationDTO;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.commontypes.NotificationReadStatus;
import rs.teslaris.core.model.commontypes.NotificationType;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.core.util.notificationhandling.NotificationAction;

@Service
public interface NotificationService extends JPAService<Notification> {

    Page<NotificationDTO> getUserNotifications(Integer userId, NotificationReadStatus readStatus,
                                             Pageable pageable);

    long getUserNotificationCount(Integer userId);

    NotificationActionResult performAction(Integer notificationId, Integer userId,
                                           NotificationAction action);

    void dismiss(Integer notificationId, Integer userId);

    void dismissAll(Integer userId);

    Notification createNotification(Notification notification);

    void cleanPastNotificationsOfType(Integer userId, NotificationType notificationType);
}
