package rs.teslaris.revisioner.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;

public interface DataQualityAssessmentRepository
    extends JpaRepository<DataQualityAssessment, Integer> {

    @Query("select a from DataQualityAssessment a join fetch a.revision where a.id = :assessmentId")
    Optional<DataQualityAssessment> findWithRevisionById(
        @Param("assessmentId") Integer assessmentId);
}
