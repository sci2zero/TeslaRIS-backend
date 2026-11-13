package rs.teslaris.exporter.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.model.document.EventsRelationType;

/**
 * A relation where the exported event is the source, for example "part of" the target event.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportEventRelation {

    @Field("type")
    private EventsRelationType type;

    @Field("target_event_id")
    private Integer targetEventId;
}
