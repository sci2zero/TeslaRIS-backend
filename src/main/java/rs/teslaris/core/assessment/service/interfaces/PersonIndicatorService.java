package rs.teslaris.core.assessment.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Service
public interface PersonIndicatorService {

    List<EntityIndicatorResponseDTO> getIndicatorsForPerson(Integer personId,
                                                            AccessLevel accessLevel);
}