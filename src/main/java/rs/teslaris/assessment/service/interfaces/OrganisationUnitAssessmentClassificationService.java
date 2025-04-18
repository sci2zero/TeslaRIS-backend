package rs.teslaris.assessment.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.EntityAssessmentClassificationResponseDTO;

@Service
public interface OrganisationUnitAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForOrganisationUnit(
        Integer organisationUnitId);
}
