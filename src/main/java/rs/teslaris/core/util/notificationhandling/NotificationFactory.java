package rs.teslaris.core.util.notificationhandling;

import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.commontypes.NotificationType;
import rs.teslaris.core.model.user.User;

@Component
public class NotificationFactory {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private static MessageSource messageSource;


    @Autowired
    public NotificationFactory(MessageSource messageSource) {
        NotificationFactory.messageSource = messageSource;
    }

    public static Notification contructNewOtherNameDetectedNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args =
            new Object[] {notificationValues.get("firstname"), notificationValues.get("middlename"),
                notificationValues.get("lastname")};
        try {
            message = messageSource.getMessage(
                "notification.newOtherNameDetected",
                args,
                Locale.forLanguageTag(user.getPreferredLanguage().getLanguageCode().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args);
        }
        return new Notification(message, notificationValues, NotificationType.ADDED_TO_PUBLICATION,
            user);
    }

    public static Notification contructAddedToPublicationNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args = new Object[] {notificationValues.get("title")};
        try {
            message = messageSource.getMessage(
                "notification.addedToPublication",
                args,
                Locale.forLanguageTag(user.getPreferredLanguage().getLanguageCode().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args);
        }
        return new Notification(message, notificationValues, NotificationType.ADDED_TO_PUBLICATION,
            user);
    }

    private static String fallbackToDefaultLocale(Object[] args) {
        return messageSource.getMessage(
            "notification.addedToPublication",
            args, DEFAULT_LOCALE
        );
    }
}
