package rs.teslaris.assessment.service.interfaces.classification;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.classification.EventAssessmentClassificationDTO;
import rs.teslaris.assessment.model.classification.EventAssessmentClassification;

@Service
public interface EventAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForEvent(
        Integer eventId);

    EventAssessmentClassification createEventAssessmentClassification(
        EventAssessmentClassificationDTO eventAssessmentClassificationDTO);

    void updateEventAssessmentClassification(Integer eventAssessmentClassificationId,
                                             EventAssessmentClassificationDTO eventAssessmentClassificationDTO);
}
