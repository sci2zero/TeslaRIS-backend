package rs.teslaris.assessment.repository.classification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.classification.EntityAssessmentClassification;

@Repository
public interface EntityAssessmentClassificationRepository
    extends JpaRepository<EntityAssessmentClassification, Integer> {
}
