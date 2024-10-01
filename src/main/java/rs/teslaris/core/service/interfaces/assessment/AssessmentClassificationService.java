package rs.teslaris.core.service.interfaces.assessment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.assessment.AssessmentClassificationDTO;
import rs.teslaris.core.model.assessment.AssessmentClassification;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface AssessmentClassificationService extends JPAService<AssessmentClassification> {

    Page<AssessmentClassificationDTO> readAllAssessmentClassifications(Pageable pageable);

    AssessmentClassificationDTO readAssessmentClassification(Integer assessmentClassificationId);

    AssessmentClassification createAssessmentClassification(
        AssessmentClassificationDTO assessmentClassification);

    void updateAssessmentClassification(Integer assessmentClassificationId,
                                        AssessmentClassificationDTO assessmentClassification);

    void deleteAssessmentClassification(Integer assessmentClassificationId);
}
