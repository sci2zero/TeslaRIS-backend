package rs.teslaris.assessment.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.EventAssessmentClassificationDTO;
import rs.teslaris.assessment.model.EventAssessmentClassification;

@Service
public interface EventAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForEvent(
        Integer eventId);

    EventAssessmentClassification createEventAssessmentClassification(
        EventAssessmentClassificationDTO eventAssessmentClassificationDTO);

    void updateEventAssessmentClassification(Integer eventAssessmentClassificationId,
                                             EventAssessmentClassificationDTO eventAssessmentClassificationDTO);
}
