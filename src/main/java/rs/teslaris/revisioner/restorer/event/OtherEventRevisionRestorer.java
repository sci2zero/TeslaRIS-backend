package rs.teslaris.revisioner.restorer.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.OtherEventDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.document.OtherEventService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class OtherEventRevisionRestorer implements RevisionRestorer<OtherEventDTO> {

    private final OtherEventService otherEventService;


    @Override
    public String entityType() {
        return EntityType.OTHER_EVENT.name();
    }

    @Override
    public Class<OtherEventDTO> dtoClass() {
        return OtherEventDTO.class;
    }

    @Override
    public void restore(Integer entityId, OtherEventDTO dto) {
        otherEventService.updateOtherEvent(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return otherEventService.readOtherEvent(entityId);
    }
}
