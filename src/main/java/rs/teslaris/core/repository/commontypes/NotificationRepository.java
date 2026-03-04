package rs.teslaris.core.repository.commontypes;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.commontypes.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId")
    List<Notification> getNotificationsForUser(Integer userId);

    @Query("SELECT n FROM Notification n WHERE " +
        "n.user.id = :userId AND " +
        "n.notificationType = :type")
    List<Notification> getNotificationsForUserAndType(Integer userId, NotificationType type);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.notificationType = 2")
    List<Notification> getNewOtherNameNotificationsForUser(Integer userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND n.notificationType = 5")
    void deleteNewPotentialClaimsNotificationsForUser(Integer userId);

    @Query("SELECT n FROM Notification n " +
        "WHERE n.user.id = :userId AND " +
        "n.user.userNotificationPeriod = 0 AND " +
        "(:notSent = FALSE OR n.sentByEmail = FALSE)")
    List<Notification> getDailyNotifications(Integer userId, boolean notSent);

    @Query("SELECT n FROM Notification n " +
        "WHERE n.user.id = :userId AND " +
        "n.user.userNotificationPeriod = 1 AND " +
        "(:notSent = FALSE OR n.sentByEmail = FALSE)")
    List<Notification> getWeeklyNotifications(Integer userId, boolean notSent);

    @Query("SELECT n FROM Notification n " +
        "WHERE n.user.id = :userId AND " +
        "n.user.userNotificationPeriod = 3 AND " +
        "(:notSent = FALSE OR n.sentByEmail = FALSE)")
    List<Notification> getMonthlyNotifications(Integer userId, boolean notSent);

    @Query("SELECT count(n) FROM Notification n WHERE n.user.id = :userId")
    long getNotificationCountForUser(Integer userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    void deleteAllForUser(Integer userId);
}
