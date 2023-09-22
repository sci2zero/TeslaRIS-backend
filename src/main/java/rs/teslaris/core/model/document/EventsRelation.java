package rs.teslaris.core.model.document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
