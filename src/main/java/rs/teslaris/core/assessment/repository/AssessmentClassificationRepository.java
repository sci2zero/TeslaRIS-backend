package rs.teslaris.core.assessment.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.AssessmentClassification;

@Repository
public interface AssessmentClassificationRepository
    extends JpaRepository<AssessmentClassification, Integer> {

    @Query("select count(eac) > 0 from EntityAssessmentClassification eac where eac.assessmentClassification.id = :assessmentClassificationId")
    boolean isInUse(Integer assessmentClassificationId);

    Optional<AssessmentClassification> findByCode(String code);
}
