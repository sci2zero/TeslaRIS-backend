package rs.teslaris.core.assessment.service.interfaces;

import java.util.List;
import rs.teslaris.core.assessment.model.EntityIndicator;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.service.interfaces.JPAService;

public interface EntityIndicatorService extends JPAService<EntityIndicator> {

    void addEntityIndicatorProof(List<DocumentFileDTO> documentFiles, Integer entityIndicatorId);

    void deleteEntityIndicatorProof(Integer entityIndicatorId, Integer proofId);

    void deleteEntityIndicator(Integer entityIndicatorId);
}
