package rs.teslaris.core.assessment.repository;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.AssessmentResearchArea;
import rs.teslaris.core.model.person.Person;

@Repository
public interface AssessmentResearchAreaRepository
    extends JpaRepository<AssessmentResearchArea, Integer> {

    @Query("SELECT ara FROM AssessmentResearchArea ara WHERE ara.person.id = :personId AND ara.commission IS NULL")
    Optional<AssessmentResearchArea> findForPersonId(Integer personId);

    @Query("SELECT ara FROM AssessmentResearchArea ara WHERE ara.person.id = :personId AND ara.commission.id = :commissionId")
    Optional<AssessmentResearchArea> findForPersonIdAndCommissionId(Integer personId,
                                                                    Integer commissionId);

    @Query("SELECT ara.person FROM AssessmentResearchArea ara WHERE ara.person.id = :personId AND ara.commission.id = :commissionId AND ara.researchAreaCode = :code")
    Set<Person> findAllForPersonIdAndCommissionIdAndCode(Integer personId, Integer commissionId,
                                                         String code);

    @Query("SELECT ara.person FROM AssessmentResearchArea ara WHERE ara.person.id = :personId AND ara.commission IS NULL AND ara.researchAreaCode = :code")
    Set<Person> findAllForPersonIdAndCode(Integer personId, String code);
}
