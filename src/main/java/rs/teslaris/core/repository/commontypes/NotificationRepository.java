package rs.teslaris.core.repository.commontypes;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND " +
        "(:read IS NULL OR (:read = TRUE AND n.readAt IS NOT NULL) OR " +
        "(:read = FALSE AND n.readAt IS NULL)) ORDER BY n.createDate DESC, n.id DESC")
    Page<Notification> getNotificationsForUserByReadStatus(Integer userId, Boolean read,
                                                         Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE " +
        "n.user.id = :userId AND " +
        "n.notificationType = :type AND n.readAt IS NULL")
    List<Notification> getNotificationsForUserAndType(Integer userId, NotificationType type);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND " +
        "n.notificationType = 2 AND n.readAt IS NULL")
    List<Notification> getNewOtherNameNotificationsForUser(Integer userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND " +
        "n.notificationType = 5 AND n.readAt IS NULL")
    void deleteNewPotentialClaimsNotificationsForUser(Integer userId);

    @Query("SELECT n FROM Notification n " +
        "WHERE n.user.id = :userId AND n.readAt IS NULL AND " +
        "n.user.userNotificationPeriod = 0 AND " +
        "(:notSent = FALSE OR n.sentByEmail = FALSE)")
    List<Notification> getDailyNotifications(Integer userId, boolean notSent);

    @Query("SELECT n FROM Notification n " +
        "WHERE n.user.id = :userId AND n.readAt IS NULL AND " +
        "n.user.userNotificationPeriod = 1 AND " +
        "(:notSent = FALSE OR n.sentByEmail = FALSE)")
    List<Notification> getWeeklyNotifications(Integer userId, boolean notSent);

    @Query("SELECT n FROM Notification n " +
        "WHERE n.user.id = :userId AND n.readAt IS NULL AND " +
        "n.user.userNotificationPeriod = 3 AND " +
        "(:notSent = FALSE OR n.sentByEmail = FALSE)")
    List<Notification> getMonthlyNotifications(Integer userId, boolean notSent);

    @Query("SELECT count(n) FROM Notification n WHERE n.user.id = :userId AND n.readAt IS NULL")
    long getNotificationCountForUser(Integer userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :readAt, n.lastModification = CURRENT_TIMESTAMP " +
        "WHERE n.user.id = :userId AND n.readAt IS NULL AND n.deleted = FALSE")
    void markAllAsReadForUser(Integer userId, LocalDateTime readAt);
}
