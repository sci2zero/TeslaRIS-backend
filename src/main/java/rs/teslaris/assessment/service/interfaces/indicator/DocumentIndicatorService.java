package rs.teslaris.assessment.service.interfaces.indicator;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.indicator.DocumentIndicatorDTO;
import rs.teslaris.assessment.dto.indicator.EntityIndicatorResponseDTO;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Service
public interface DocumentIndicatorService {

    List<EntityIndicatorResponseDTO> getIndicatorsForDocument(Integer documentId,
                                                              AccessLevel accessLevel);

    DocumentIndicator createDocumentIndicator(DocumentIndicatorDTO documentIndicatorDTO,
                                              Integer userId);

    void updateDocumentIndicator(Integer documentIndicatorId,
                                 DocumentIndicatorDTO documentIndicatorDTO);
}
