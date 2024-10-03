package rs.teslaris.core.assessment.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.DocumentIndicatorDTO;
import rs.teslaris.core.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.core.assessment.model.DocumentIndicator;
import rs.teslaris.core.assessment.model.IndicatorAccessLevel;

@Service
public interface DocumentIndicatorService {

    List<EntityIndicatorResponseDTO> getIndicatorsForDocument(Integer documentId,
                                                              IndicatorAccessLevel accessLevel);

    DocumentIndicator createDocumentIndicator(DocumentIndicatorDTO documentIndicatorDTO);

    void updateDocumentIndicator(Integer documentIndicatorId,
                                 DocumentIndicatorDTO documentIndicatorDTO);
}
