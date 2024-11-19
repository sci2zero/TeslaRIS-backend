package rs.teslaris.core.repository.commontypes;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("select n from Notification n where n.user.id = :userId")
    List<Notification> getNotificationsForUser(Integer userId);

    @Query("select n from Notification n where n.user.id = :userId and n.notificationType = 2")
    List<Notification> getNewOtherNameNotificationsForUser(Integer userId);

    @Modifying
    @Query("delete from Notification n where n.user.id = :userId and n.notificationType = 5")
    void deleteNewPotentialClaimsNotificationsForUser(Integer userId);

    @Query("select n from Notification n where n.user.id = :userId and n.user.userNotificationPeriod = 0")
    List<Notification> getDailyNotifications(Integer userId);

    @Query("select n from Notification n where n.user.id = :userId and n.user.userNotificationPeriod = 1")
    List<Notification> getWeeklyNotifications(Integer userId);

    @Query("select count(n) from Notification n where n.user.id = :userId")
    long getNotificationCountForUser(Integer userId);
}
