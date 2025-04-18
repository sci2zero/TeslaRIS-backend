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

    @Query("select am from AssessmentMeasure am join fetch am.title t where t.content like %:searchExpression%")
    Page<AssessmentMeasure> searchAssessmentMeasures(Pageable pageable, String searchExpression);
}
