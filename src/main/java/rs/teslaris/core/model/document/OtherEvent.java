package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.indexmodel.EventType;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "other_events")
@SQLRestriction("deleted=false")
public class OtherEvent extends Event {

    @Column(name = "type")
    private OtherEventType type;

    public OtherEvent() {
        this.setEventType(EventType.OTHER_EVENT);
    }
}
