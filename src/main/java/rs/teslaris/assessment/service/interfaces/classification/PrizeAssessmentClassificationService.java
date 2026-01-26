package rs.teslaris.assessment.service.interfaces.classification;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.classification.PrizeAssessmentClassificationDTO;

@Service
public interface PrizeAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPrize(
        Integer prizeId);

    EntityAssessmentClassificationResponseDTO createPrizeAssessmentClassification(
        PrizeAssessmentClassificationDTO prizeAssessmentClassificationDTO);

    void editPrizeAssessmentClassification(Integer classificationId,
                                           PrizeAssessmentClassificationDTO prizeAssessmentClassificationDTO);
}
