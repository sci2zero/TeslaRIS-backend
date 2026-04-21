package rs.teslaris.core.service.interfaces.identifier;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.dto.identifier.EventIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.EventIdentifier;

@Service
public interface EventIdentifierService {

    List<EntityIdentifierResponseDTO> getIdentifiersForEvent(Integer eventId,
                                                             AccessLevel accessLevel);

    EventIdentifier createEventIdentifier(EventIdentifierDTO eventIdentifierDTO, Integer userId);

    void updateEventIdentifier(Integer eventIdentifierId, EventIdentifierDTO eventIdentifierDTO);
}
