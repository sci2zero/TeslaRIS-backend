package rs.teslaris.assessment.service.interfaces.classification;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.classification.AssessmentClassificationDTO;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface AssessmentClassificationService extends JPAService<AssessmentClassification> {

    Page<AssessmentClassificationDTO> readAllAssessmentClassifications(Pageable pageable,
                                                                       String language);

    List<AssessmentClassificationDTO> getAssessmentClassificationsApplicableToEntity(
        List<ApplicableEntityType> applicableEntityTypes);

    AssessmentClassificationDTO readAssessmentClassification(Integer assessmentClassificationId);

    AssessmentClassification createAssessmentClassification(
        AssessmentClassificationDTO assessmentClassification);

    void updateAssessmentClassification(Integer assessmentClassificationId,
                                        AssessmentClassificationDTO assessmentClassification);

    void deleteAssessmentClassification(Integer assessmentClassificationId);

    AssessmentClassification readAssessmentClassificationByCode(String code);
}
