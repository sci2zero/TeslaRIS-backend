package rs.teslaris.core.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.PublicationSeriesAssessmentClassification;

@Repository
public interface PublicationSeriesAssessmentClassificationRepository extends
    JpaRepository<PublicationSeriesAssessmentClassification, Integer> {
}
