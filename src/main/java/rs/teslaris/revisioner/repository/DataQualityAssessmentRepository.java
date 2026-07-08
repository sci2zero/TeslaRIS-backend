package rs.teslaris.revisioner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;

public interface DataQualityAssessmentRepository
    extends JpaRepository<DataQualityAssessment, Integer> {
}
