package rs.teslaris.core.model.commontypes;

import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.user.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification")
@Where(clause = "deleted=false")
public class Notification extends BaseEntity {

    @Column(name = "notification_text", nullable = false)
    private String notificationText;

    @ElementCollection
    @CollectionTable(name = "notification_values_mapping",
        joinColumns = {@JoinColumn(name = "notification_id", referencedColumnName = "id")})
    @MapKeyColumn(name = "item_value")
    @Column(name = "values", nullable = false)
    private Map<String, String> values;

    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
}
