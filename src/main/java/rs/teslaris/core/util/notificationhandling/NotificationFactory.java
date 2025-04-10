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
        return new Notification(message, notificationValues,
            NotificationType.NEW_OTHER_NAME_DETECTED,
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

    public static Notification contructNewImportsNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args =
            new Object[] {notificationValues.get("newImportCount")};
        try {
            message = messageSource.getMessage(
                "notification.newImportsHarvested",
                args,
                Locale.forLanguageTag(user.getPreferredLanguage().getLanguageCode().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args);
        }
        return new Notification(message, notificationValues, NotificationType.NEW_IMPORTS_HARVESTED,
            user);
    }

    public static Notification contructNewDeduplicationScanFinishedNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args =
            new Object[] {notificationValues.get("duplicateCount")};
        try {
            message = messageSource.getMessage(
                "notification.deduplicationScanFinished",
                args,
                Locale.forLanguageTag(user.getPreferredLanguage().getLanguageCode().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args);
        }
        return new Notification(message, notificationValues,
            NotificationType.DEDUPLICATION_SCAN_FINISHED, user);
    }

    public static Notification contructNewPotentialClaimsFoundNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args =
            new Object[] {notificationValues.get("potentialClaimsNumber")};
        try {
            message = messageSource.getMessage(
                "notification.potentialClaimsFound",
                args,
                Locale.forLanguageTag(user.getPreferredLanguage().getLanguageCode().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args);
        }
        return new Notification(message, notificationValues,
            NotificationType.FOUND_POTENTIAL_CLAIMS, user);
    }

    public static Notification contructNewEventsForClassificationNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args =
            new Object[] {notificationValues.get("totalCount"),
                notificationValues.get("fromMyInstitutionCount")};
        try {
            message = messageSource.getMessage(
                "notification.newEventsForClassification",
                args,
                Locale.forLanguageTag(user.getPreferredLanguage().getLanguageCode().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args);
        }
        return new Notification(message, notificationValues,
            NotificationType.NEW_EVENTS_TO_CLASSIFY, user);
    }

    public static Notification contructNewPublicationsForAssessmentNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args =
            new Object[] {notificationValues.get("totalCount"),
                notificationValues.get("fromMyInstitutionCount")};
        try {
            message = messageSource.getMessage(
                "notification.newPublicationsForAssessment",
                args,
                Locale.forLanguageTag(user.getPreferredLanguage().getLanguageCode().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args);
        }
        return new Notification(message, notificationValues,
            NotificationType.NEW_PUBLICATIONS_TO_ASSESS, user);
    }

    public static Notification contructCandidatePulledFromPromotionNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args =
            new Object[] {notificationValues.get("candidateName"),
                notificationValues.get("promotionDate")};
        try {
            message = messageSource.getMessage(
                "promotion.candidatePulledFromPromotion",
                args,
                Locale.forLanguageTag(user.getPreferredLanguage().getLanguageCode().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args);
        }
        return new Notification(message, notificationValues,
            NotificationType.PROMOTION_NOTIFICATION, user);
    }

    public static Notification contructScheduledTaskCompletedNotification(
        Map<String, String> notificationValues, User user, boolean success) {
        String message;
        var args =
            new Object[] {notificationValues.get("taskId"), notificationValues.get("duration")};
        try {
            message = messageSource.getMessage(
                success ? "notification.scheduleTaskCompleted" : "notification.scheduleTaskFailed",
                args,
                Locale.forLanguageTag(user.getPreferredLanguage().getLanguageCode().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args);
        }
        return new Notification(message, notificationValues,
            NotificationType.SCHEDULED_TASK_COMPLETED, user);
    }

    private static String fallbackToDefaultLocale(Object[] args) {
        return messageSource.getMessage(
            "notification.addedToPublication",
            args, DEFAULT_LOCALE
        );
    }
}
