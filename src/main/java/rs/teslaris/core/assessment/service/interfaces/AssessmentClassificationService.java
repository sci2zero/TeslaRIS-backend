package rs.teslaris.core.assessment.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.AssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.AssessmentClassification;
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

    AssessmentClassification readAssessmentClassificationByCode(String code);
}
