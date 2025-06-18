package rs.teslaris.assessment.service.interfaces.classification;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;

@Service
public interface OrganisationUnitAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForOrganisationUnit(
        Integer organisationUnitId);
}
