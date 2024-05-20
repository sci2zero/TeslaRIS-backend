package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "events_relation")
@Where(clause = "deleted=false")
public class EventsRelation extends BaseEntity {

    @Column(name = "events_relation_type")
    private EventsRelationType eventsRelationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_event_id")
    private Event source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_event_id")
    private Event target;
}
