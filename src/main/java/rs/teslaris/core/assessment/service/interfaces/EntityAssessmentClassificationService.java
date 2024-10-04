package rs.teslaris.core.assessment.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.model.EntityAssessmentClassification;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface EntityAssessmentClassificationService
    extends JPAService<EntityAssessmentClassification> {

    void deleteEntityAssessmentClassification(Integer entityAssessmentId);
}
