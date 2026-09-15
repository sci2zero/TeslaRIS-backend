package rs.teslaris.revisioner.restorer.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class ConferenceRevisionRestorer implements RevisionRestorer<ConferenceDTO> {

    private final ConferenceService conferenceService;


    @Override
    public String entityType() {
        return EntityType.CONFERENCE.name();
    }

    @Override
    public Class<ConferenceDTO> dtoClass() {
        return ConferenceDTO.class;
    }

    @Override
    public void restore(Integer entityId, ConferenceDTO dto) {
        conferenceService.updateConference(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return conferenceService.readConference(entityId);
    }
}
