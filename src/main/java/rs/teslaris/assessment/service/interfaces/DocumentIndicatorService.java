package rs.teslaris.assessment.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.DocumentIndicatorDTO;
import rs.teslaris.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.assessment.model.DocumentIndicator;
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
