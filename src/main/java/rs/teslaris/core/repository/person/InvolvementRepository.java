package rs.teslaris.core.repository.person;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;

@Repository
public interface InvolvementRepository extends JpaRepository<Involvement, Integer> {

    @Query("SELECT e FROM Employment e " +
        "WHERE e.personInvolved.id = :personId " +
        "AND e.organisationUnit.id = :institutionId " +
        "AND e.dateTo IS NULL")
    Optional<Employment> findActiveEmploymentForPersonAndInstitution(Integer institutionId,
                                                                     Integer personId);

    @Query("SELECT e FROM Employment e " +
        "JOIN FETCH e.organisationUnit WHERE " +
        "e.organisationUnit IS NOT NULL AND " +
        "e.personInvolved.id = :personId")
    List<Employment> findEmploymentsForPerson(Integer personId);

    @Query("SELECT DISTINCT e.organisationUnit.id FROM Employment e " +
        "WHERE e.personInvolved.id = :personId " +
        "AND e.dateTo IS NULL")
    List<Integer> findActiveEmploymentInstitutionIds(Integer personId);

    @Query(
        "SELECT e FROM Employment e LEFT JOIN FETCH e.organisationUnit " +
            "WHERE e.personInvolved.id = :personId " +
            "AND e.dateTo IS NULL " +
            "AND e.organisationUnit IS NOT NULL")
    List<Employment> findActiveEmploymentInstitutions(Integer personId);

    @Query("SELECT e FROM Employment e LEFT JOIN FETCH e.organisationUnit " +
        "WHERE e.personInvolved.id = :personId " +
        "AND e.organisationUnit.id IN :institutionIds " +
        "AND e.dateTo IS NULL")
    List<Employment> findActiveEmploymentsForPersonAndInstitutions(List<Integer> institutionIds,
                                                                   Integer personId);

    @Query("SELECT e FROM Employment e LEFT JOIN FETCH e.organisationUnit " +
        "WHERE e.organisationUnit.id IN :institutionIds " +
        "AND e.dateTo IS NULL ORDER BY e.id")
    List<Employment> findActiveEmploymentsForInstitutions(List<Integer> institutionIds);

    @Query("SELECT COUNT(e) FROM Employment e LEFT JOIN e.organisationUnit " +
        "WHERE e.organisationUnit.id IN :institutionIds " +
        "AND e.dateTo IS NULL")
    Integer countActiveEmploymentsForInstitutions(List<Integer> institutionIds);

    @Query("SELECT i FROM Involvement i WHERE i.personInvolved.id = :personId ORDER BY i.id")
    List<Involvement> findByPersonInvolvedIdOrderById(Integer personId);

    /**
     * An involvement counts as an activity on the same terms a contribution does: it has to carry
     * at least one of a start date, an end date or a research area.
     */
    @Query("SELECT COUNT(DISTINCT i.id) FROM Involvement i LEFT JOIN i.researchAreas ra " +
        "WHERE i.personInvolved.id = :personId " +
        "AND (i.dateFrom IS NOT NULL OR i.dateTo IS NOT NULL OR ra.id IS NOT NULL)")
    Integer countActivitiesForPerson(Integer personId);

    @Query("""
            SELECT i.id AS id,
                   i.involvementType AS involvementType,
                   i.dateFrom AS dateFrom
            FROM Involvement i
            WHERE i.personInvolved.id = :personId
            ORDER BY i.dateFrom DESC NULLS LAST
        """)
    List<InvolvementIdTypeDate> findIdsTypesAndDatesByPersonId(Integer personId);

    interface InvolvementIdTypeDate {
        Integer getId();

        InvolvementType getInvolvementType();

        LocalDate getDateFrom();
    }
}
