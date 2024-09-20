package rs.teslaris.core.repository.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.assessment.PublicationSeriesAssessmentClassification;

@Repository
public interface PublicationSeriesAssessmentClassificationRepository extends
    JpaRepository<PublicationSeriesAssessmentClassification, Integer> {
}
