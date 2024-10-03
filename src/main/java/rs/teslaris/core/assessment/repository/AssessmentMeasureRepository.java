package rs.teslaris.core.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.AssessmentMeasure;

@Repository
public interface AssessmentMeasureRepository extends
    JpaRepository<AssessmentMeasure, Integer> {

    @Query("select count(ar) > 0 from AssessmentRulebook ar where ar.assessmentMeasure.id = :assessmentMeasureId")
    boolean isInUse(Integer assessmentMeasureId);
}
