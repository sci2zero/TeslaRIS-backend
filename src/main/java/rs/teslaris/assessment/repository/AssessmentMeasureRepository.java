package rs.teslaris.assessment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.AssessmentMeasure;

@Repository
public interface AssessmentMeasureRepository extends
    JpaRepository<AssessmentMeasure, Integer> {

    @Query("SELECT am FROM AssessmentMeasure am " +
        "JOIN fetch am.title t " +
        "WHERE t.content LIKE %:searchExpression%")
    Page<AssessmentMeasure> searchAssessmentMeasures(Pageable pageable, String searchExpression);
}
