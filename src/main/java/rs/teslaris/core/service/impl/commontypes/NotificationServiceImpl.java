package rs.teslaris.core.service.impl.commontypes;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.commontypes.NotificationDTO;
import rs.teslaris.core.indexmodel.UserAccountIndex;
import rs.teslaris.core.indexrepository.UserAccountIndexRepository;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.commontypes.NotificationType;
import rs.teslaris.core.model.user.UserNotificationPeriod;
import rs.teslaris.core.repository.commontypes.NotificationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NotificationException;
import rs.teslaris.core.util.notificationhandling.NotificationAction;
import rs.teslaris.core.util.notificationhandling.NotificationConfiguration;
import rs.teslaris.core.util.notificationhandling.handlerimpl.AddedToPublicationNotificationHandler;
import rs.teslaris.core.util.notificationhandling.handlerimpl.NewOtherNameNotificationHandler;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl extends JPAServiceImpl<Notification>
    implements NotificationService {

    private final MessageSource messageSource;

    private final NotificationRepository notificationRepository;

    private final NewOtherNameNotificationHandler newOtherNameNotificationHandler;

    private final AddedToPublicationNotificationHandler addedToPublicationNotificationHandler;

    private final UserAccountIndexRepository userAccountIndexRepository;

    private final EmailUtil emailUtil;

    @Value("${frontend.application.address}")
    private String clientAppAddress;


    @Override
    protected JpaRepository<Notification, Integer> getEntityRepository() {
        return notificationRepository;
    }

    @Override
    public List<NotificationDTO> getUserNotifications(Integer userId) {
        var notificationList = notificationRepository.getNotificationsForUser(userId);

        return notificationList.stream().map(
                notification -> new NotificationDTO(notification.getId(),
                    notification.getNotificationText(),
                    NotificationConfiguration.allowedActions.get(notification.getNotificationType())))
            .collect(
                Collectors.toList());
    }

    @Override
    public long getUserNotificationCount(Integer userId) {
        return notificationRepository.getNotificationCountForUser(userId);
    }

    @Override
    public void performAction(Integer notificationId, Integer userId,
                              NotificationAction notificationAction) {
        var notification = findOne(notificationId);

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotificationException(
                "Exception you are trying to approve does not belong to you.");
        }

        switch (notification.getNotificationType()) {
            case NEW_PAPER_HARVESTED:
                //TODO: To be implemented...
                break;
            case NEW_OTHER_NAME_DETECTED:
                newOtherNameNotificationHandler.handle(notification, notificationAction);
                break;
            case ADDED_TO_PUBLICATION:
                addedToPublicationNotificationHandler.handle(notification, notificationAction);
                break;
        }

        delete(notificationId);
    }

    @Override
    public void dismiss(Integer notificationId, Integer userId) {
        var notification = findOne(notificationId);

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotificationException(
                "Exception you are trying to approve does not belong to you.");
        }

        delete(notificationId);
    }

    @Override
    @Nullable
    public Notification createNotification(Notification notification) {
        if (Objects.requireNonNull(notification.getNotificationType())
            .equals(NotificationType.NEW_OTHER_NAME_DETECTED)) {
            var newOtherNameNotifications =
                notificationRepository.getNewOtherNameNotificationsForUser(
                    notification.getUser().getId());
            for (var oldNotification : newOtherNameNotifications) {
                if (oldNotification.getValues().get("firstname")
                    .equals(notification.getValues().get("firstname")) &&
                    oldNotification.getValues().get("middlename")
                        .equals(notification.getValues().get("middlename")) &&
                    oldNotification.getValues().get("lastname")
                        .equals(notification.getValues().get("lastname"))) {
                    return null;
                }
            }
        }

        return notificationRepository.save(notification);
    }

    @Scheduled(cron = "${notifications.schedule.daily}")
    protected void sendDailyNotifications() {
        sendNotifications(UserNotificationPeriod.DAILY);
    }

    @Scheduled(cron = "${notifications.schedule.weekly}")
    protected void sendWeeklyNotifications() {
        sendNotifications(UserNotificationPeriod.WEEKLY);
    }

    private void sendNotifications(UserNotificationPeriod notificationPeriod) {
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<UserAccountIndex> chunk = userAccountIndexRepository
                .findAll(PageRequest.of(pageNumber, chunkSize))
                .getContent();

            chunk.forEach(accountIndex -> {
                var notifications = fetchNotifications(notificationPeriod, accountIndex);

                if (notifications.isEmpty()) {
                    return;
                }

                var locale = getLocale(notifications);
                var emailContent = buildEmailContent(notificationPeriod, notifications, locale);
                var subject =
                    messageSource.getMessage(getSubjectKey(notificationPeriod), null, locale);

                emailUtil.sendSimpleEmail(accountIndex.getEmail(), subject, emailContent);
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private List<Notification> fetchNotifications(UserNotificationPeriod notificationPeriod,
                                                  UserAccountIndex accountIndex) {
        return notificationPeriod == UserNotificationPeriod.DAILY
            ? notificationRepository.getDailyNotifications(accountIndex.getDatabaseId())
            : notificationRepository.getWeeklyNotifications(accountIndex.getDatabaseId());
    }

    private Locale getLocale(List<Notification> notifications) {
        var language =
            notifications.getFirst().getUser().getPreferredLanguage().getLanguageCode()
                .toLowerCase();
        return Locale.forLanguageTag(language);
    }

    private String buildEmailContent(UserNotificationPeriod notificationPeriod,
                                     List<Notification> notifications, Locale locale) {
        var stringBuilder = new StringBuilder();

        stringBuilder.append(
                messageSource.getMessage(getStartKey(notificationPeriod), null, locale))
            .append("\n\n");

        notifications.forEach(notification -> {
            stringBuilder.append(notification.getNotificationText()).append("\n\n");
        });

        stringBuilder.append(
                messageSource.getMessage("notification.forMoreInfoMailEnd", null, locale))
            .append(" ")
            .append(clientAppAddress)
            .append(clientAppAddress.endsWith("/") ? locale.toLanguageTag().toLowerCase() :
                "/" + locale.toLanguageTag().toLowerCase()).append("/notifications");

        return stringBuilder.toString();
    }

    private String getStartKey(UserNotificationPeriod notificationPeriod) {
        return notificationPeriod == UserNotificationPeriod.DAILY
            ? "notification.dailyMailStart"
            : "notification.weeklyMailStart";
    }

    private String getSubjectKey(UserNotificationPeriod notificationPeriod) {
        return notificationPeriod == UserNotificationPeriod.DAILY
            ? "notification.dailyMailSubject"
            : "notification.weeklyMailSubject";
    }
}
