package rs.teslaris.core.service.interfaces.commontypes;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.NotificationDTO;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface NotificationService extends JPAService<Notification> {

    List<NotificationDTO> getUserNotifications(Integer userId);

    long getUserNotificationCount(Integer userId);

    void approve(Integer notificationId, Integer userId);

    void reject(Integer notificationId, Integer userId);

    Notification createNotification(Notification notification);
}
