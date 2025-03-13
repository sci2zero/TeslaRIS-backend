package rs.teslaris.core.model.commontypes;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.validator.constraints.Length;
import rs.teslaris.core.model.user.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification")
@SQLRestriction("deleted=false")
public class Notification extends BaseEntity {

    @Length(max = 10000)
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
