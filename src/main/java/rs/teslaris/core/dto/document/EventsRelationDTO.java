package rs.teslaris.core.dto.document;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.EventsRelationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventsRelationDTO {

    private Integer id;

    private Integer sourceId;

    private Integer targetId;

    private List<MultilingualContentDTO> sourceEventName;

    private List<MultilingualContentDTO> targetEventName;

    private EventsRelationType eventsRelationType;
}
