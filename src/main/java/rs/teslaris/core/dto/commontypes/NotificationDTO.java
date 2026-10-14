package rs.teslaris.core.dto.commontypes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.NotificationSentiment;
import rs.teslaris.core.util.notificationhandling.NotificationAction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private Integer id;

    private String notificationText;

    private String displayValue;

    private List<NotificationAction> possibleActions;

    private LocalDateTime creationTimestamp;

    private LocalDateTime readAt;

    private String details;

    private NotificationSentiment sentiment;

    public OffsetDateTime getCreationTimestamp() {
        return creationTimestamp == null ? null :
            creationTimestamp.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    public OffsetDateTime getReadAt() {
        return readAt == null ? null : readAt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
