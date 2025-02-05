package rs.teslaris.core.assessment.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.AssessmentResearchArea;

@Repository
public interface AssessmentResearchAreaRepository
    extends JpaRepository<AssessmentResearchArea, Integer> {

    @Query("SELECT ara FROM AssessmentResearchArea ara WHERE ara.person.id = :personId")
    Optional<AssessmentResearchArea> findForPersonId(Integer personId);
}
