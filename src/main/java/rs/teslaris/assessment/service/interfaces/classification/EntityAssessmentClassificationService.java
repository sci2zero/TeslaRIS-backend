package rs.teslaris.assessment.service.interfaces.classification;

import org.springframework.stereotype.Service;
import rs.teslaris.assessment.model.classification.EntityAssessmentClassification;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface EntityAssessmentClassificationService
    extends JPAService<EntityAssessmentClassification> {

    void deleteEntityAssessmentClassification(Integer entityAssessmentId);
}
