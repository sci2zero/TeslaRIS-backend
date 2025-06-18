package rs.teslaris.assessment.service.interfaces.indicator;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.indicator.EntityIndicatorResponseDTO;
import rs.teslaris.assessment.dto.indicator.EventIndicatorDTO;
import rs.teslaris.assessment.model.indicator.EventIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Service
public interface EventIndicatorService {

    List<EntityIndicatorResponseDTO> getIndicatorsForEvent(Integer eventId,
                                                           AccessLevel accessLevel);

    EventIndicator createEventIndicator(EventIndicatorDTO eventIndicatorDTO, Integer userId);

    void updateEventIndicator(Integer eventIndicatorId, EventIndicatorDTO eventIndicatorDTO);
}
