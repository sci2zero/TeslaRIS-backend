package rs.teslaris.core.assessment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.AssessmentMeasure;

@Repository
public interface AssessmentMeasureRepository extends
    JpaRepository<AssessmentMeasure, Integer> {

    @Query("select count(ar) > 0 from AssessmentRulebook ar join ar.assessmentMeasures am where am.id = :assessmentMeasureId")
    boolean isInUse(Integer assessmentMeasureId);

    @Query("select am from AssessmentMeasure am where am.code like '%:searchExpression%'")
    Page<AssessmentMeasure> searchAssessmentMeasures(Pageable pageable, String searchExpression);
}
