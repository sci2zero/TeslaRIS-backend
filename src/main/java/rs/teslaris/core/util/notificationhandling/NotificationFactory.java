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
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "notification.newOtherNameDetected");
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
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "notification.addedToPublication");
        }
        return new Notification(message, notificationValues, NotificationType.ADDED_TO_PUBLICATION,
            user);
    }

    public static Notification constructAuthorUnbindedFromPublicationNotification(
        Map<String, String> notificationValues, User user, boolean notifyingEditor) {
        String message;
        var args = new Object[] {notificationValues.get("author"), notificationValues.get("title")};

        var messageCode = notifyingEditor ? "notification.researcherUnbindedFromPublication" :
            "notification.unbindedFromPublication";
        try {
            message = messageSource.getMessage(
                messageCode,
                args,
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, messageCode);
        }

        return new Notification(message, notificationValues,
            notifyingEditor ? NotificationType.NEW_EMPLOYED_RESEARCHER_UNBINDED :
                NotificationType.NEW_AUTHOR_UNBINDING,
            user);
    }

    public static Notification constructAuthorUnbindedByEditorNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args = new Object[] {notificationValues.get("title")};

        try {
            message = messageSource.getMessage(
                "notification.unbindedByEditor",
                args,
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "notification.unbindedByEditor");
        }

        return new Notification(message, notificationValues,
            NotificationType.AUTHOR_UNBINDED_BY_EDITOR, user);
    }

    public static Notification constructAllAuthorsUnbindedNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args = new Object[] {notificationValues.get("title")};
        try {
            message = messageSource.getMessage(
                "notification.allAuthorsUnbinded",
                args,
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "notification.allAuthorsUnbinded");
        }
        return new Notification(message, notificationValues, NotificationType.ALL_AUTHORS_UNBINDED,
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
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "notification.newImportsHarvested");
        }
        return new Notification(message, notificationValues, NotificationType.NEW_IMPORTS_HARVESTED,
            user);
    }

    public static Notification contructNewEntityCreationNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args =
            new Object[] {
                notificationValues.get("entityType"),
                notificationValues.get("entityName")
            };

        try {
            message = messageSource.getMessage(
                "notification.newEntityCreation",
                args,
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "notification.newEntityCreation");
        }
        return new Notification(message, notificationValues, NotificationType.NEW_ENTITY_CREATION,
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
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "notification.deduplicationScanFinished");
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
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "notification.potentialClaimsFound");
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
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "notification.newEventsForClassification");
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
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "notification.newPublicationsForAssessment");
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
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "promotion.candidatePulledFromPromotion");
        }
        return new Notification(message, notificationValues,
            NotificationType.PROMOTION_NOTIFICATION, user);
    }

    public static Notification contructScheduledTaskCompletedNotification(
        Map<String, String> notificationValues, User user, boolean success) {
        String message;
        var args =
            new Object[] {notificationValues.get("taskId"), notificationValues.get("duration")};

        var messageCode =
            success ? "notification.scheduleTaskCompleted" : "notification.scheduleTaskFailed";
        try {
            message = messageSource.getMessage(
                messageCode,
                args,
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, messageCode);
        }
        return new Notification(message, notificationValues,
            NotificationType.SCHEDULED_TASK_COMPLETED, user);
    }

    public static Notification contructScheduledReportGenerationCompletedNotification(
        Map<String, String> notificationValues, User user, boolean success) {
        String message;
        var args =
            new Object[] {notificationValues.get("duration")};

        var messageCode = success ? "notification.scheduleReportGenerationCompleted" :
            "notification.scheduleReportGenerationFailed";
        try {
            message = messageSource.getMessage(
                messageCode,
                args,
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, messageCode);
        }
        return new Notification(message, notificationValues,
            NotificationType.SCHEDULED_TASK_COMPLETED, user);
    }

    public static Notification contructScheduledBackupGenerationCompletedNotification(
        Map<String, String> notificationValues, User user, boolean success) {
        String message;
        var args =
            new Object[] {notificationValues.get("duration")};

        var messageCode = success ? "notification.scheduleBackupGenerationCompleted" :
            "notification.scheduleBackupGenerationFailed";
        try {
            message = messageSource.getMessage(
                messageCode,
                args,
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, messageCode);
        }
        return new Notification(message, notificationValues,
            NotificationType.SCHEDULED_TASK_COMPLETED, user);
    }

    public static Notification contructNewDocumentsForValidationNotification(
        Map<String, String> notificationValues, User user) {
        String message;
        var args =
            new Object[] {notificationValues.get("nonValidatedDocumentsCount")};
        try {
            message = messageSource.getMessage(
                "notification.documentsForValidation",
                args,
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            );
        } catch (NoSuchMessageException e) {
            message = fallbackToDefaultLocale(args, "notification.documentsForValidation");
        }
        return new Notification(message, notificationValues,
            NotificationType.NEW_DOCUMENTS_FOR_VALIDATION, user);
    }

    private static String fallbackToDefaultLocale(Object[] args, String messageCode) {
        return messageSource.getMessage(
            messageCode,
            args, DEFAULT_LOCALE
        );
    }
}
