package rs.teslaris.core.dto.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.EventsRelationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventsRelationDTO {

    private Integer sourceId;

    private Integer targetId;

    private EventsRelationType eventsRelationType;
}
