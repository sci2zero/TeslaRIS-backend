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
                    "Neko vas je dodao na publikaciju sa novim oblikom imena (" +
                        notificationValues.get("firstname") + " " +
                        notificationValues.get("middlename") + " " +
                        notificationValues.get("lastname") +
                        "), hoćete li da dodate ovo ime u listu drugin oblika imena?",
                    notificationValues, NotificationType.NEW_OTHER_NAME_DETECTED, user);
            case "EN":
                return new Notification(
                    "Someone added you to a publication with name (" +
                        notificationValues.get("firstname") + " " +
                        notificationValues.get("middlename") + " " +
                        notificationValues.get("lastname") +
                        "), do you want to add it to your other name list?",
                    notificationValues, NotificationType.NEW_OTHER_NAME_DETECTED, user);

        }
        return null;
    }
}
