package rs.teslaris.core.assessment.repository;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.AssessmentResearchArea;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;

@Repository
public interface AssessmentResearchAreaRepository
    extends JpaRepository<AssessmentResearchArea, Integer> {

    @Query("SELECT ara FROM AssessmentResearchArea ara WHERE ara.person.id = :personId AND ara.commission IS NULL")
    Optional<AssessmentResearchArea> findForPersonId(Integer personId);

    @Query("SELECT ara FROM AssessmentResearchArea ara WHERE ara.person.id = :personId AND ara.commission.id = :commissionId")
    Optional<AssessmentResearchArea> findForPersonIdAndCommissionId(Integer personId,
                                                                    Integer commissionId);

    @Query("""
            SELECT p
            FROM Person p
            WHERE (
                p.id IN (
                    SELECT ara.person.id
                    FROM AssessmentResearchArea ara
                    WHERE ara.commission.id = :commissionId
                    AND ara.researchAreaCode = :code
                )
                OR (
                    p.id IN (
                        SELECT ara.person.id
                        FROM AssessmentResearchArea ara
                        WHERE ara.commission IS NULL
                        AND ara.researchAreaCode = :code
                    )
                    AND NOT EXISTS (
                        SELECT 1
                        FROM AssessmentResearchArea ara2
                        WHERE ara2.person.id = p.id
                        AND ara2.commission.id = :commissionId
                    )
                )
            )
            AND EXISTS (
                SELECT 1
                FROM Involvement i
                WHERE i.personInvolved.id = p.id
                AND i.involvementType IN :involvementTypes
                AND i.organisationUnit.id = :organisationUnitId
            )
            AND p NOT IN (
                SELECT e
                FROM Commission c JOIN c.excludedResearchers e
                WHERE c.id = :commissionId
            )
        """)
    Set<Person> findPersonsForAssessmentResearchArea(Integer commissionId, String code,
                                                     Set<InvolvementType> involvementTypes,
                                                     Integer organisationUnitId);
}
