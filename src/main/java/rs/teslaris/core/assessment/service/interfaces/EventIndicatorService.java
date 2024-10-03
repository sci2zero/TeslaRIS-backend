package rs.teslaris.core.assessment.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.core.assessment.model.IndicatorAccessLevel;

@Service
public interface EventIndicatorService {

    List<EntityIndicatorResponseDTO> getIndicatorsForEvent(Integer eventId,
                                                           IndicatorAccessLevel accessLevel);
}
