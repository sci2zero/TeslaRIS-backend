package rs.teslaris.core.util.notificationhandling;

import java.util.Map;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.commontypes.NotificationType;
import rs.teslaris.core.model.user.User;

public class NotificationFactory {

    public static Notification contructNewOtherNameDetectedNotification(
        Map<String, String> notificationValues, User user) {
        switch (user.getPreferredLanguage().getLanguageCode()) {
            case "SR":
                return new Notification(
                    "Dodati ste na publikaciju sa novim oblikom imena (" +
                        notificationValues.get("firstname") + " " +
                        notificationValues.get("middlename") + " " +
                        notificationValues.get("lastname") +
                        "), hoćete li da dodate ovo ime u listu drugin oblika imena?",
                    notificationValues, NotificationType.NEW_OTHER_NAME_DETECTED, user);
            default:
                return new Notification(
                    "You are added to a publication with a new name variant (" +
                        notificationValues.get("firstname") + " " +
                        notificationValues.get("middlename") + " " +
                        notificationValues.get("lastname") +
                        "), do you want to add it to your other name list?",
                    notificationValues, NotificationType.NEW_OTHER_NAME_DETECTED, user);
        }
    }

    public static Notification contructAddedToPublicationNotification(
        Map<String, String> notificationValues, User user) {
        switch (user.getPreferredLanguage().getLanguageCode()) {
            case "SR":
                return new Notification(
                    "Neko vas je dodao na publikaciju (" +
                        notificationValues.get("title") +
                        "), ukoliko ova publikacija nije vaša, možete se ukloniti sa nje.",
                    notificationValues, NotificationType.ADDED_TO_PUBLICATION, user);
            default:
                return new Notification(
                    "Someone added zou to publication (" +
                        notificationValues.get("title") +
                        "), if this is not your publication, you can remove yourself from it.",
                    notificationValues, NotificationType.ADDED_TO_PUBLICATION, user);
        }
    }
}
