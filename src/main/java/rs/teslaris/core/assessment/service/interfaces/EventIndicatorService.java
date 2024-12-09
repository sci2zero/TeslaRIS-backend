package rs.teslaris.core.assessment.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Service
public interface EventIndicatorService {

    List<EntityIndicatorResponseDTO> getIndicatorsForEvent(Integer eventId,
                                                           AccessLevel accessLevel);
}
