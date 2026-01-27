package rs.teslaris.core.repository.person;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Employment;

@Repository
public interface EmploymentRepository extends JpaRepository<Employment, Integer> {

    @Query("SELECT e FROM Employment e WHERE e.personInvolved.id = :personId ORDER BY e.id")
    List<Employment> findByPersonInvolvedId(Integer personId);

    @Query("SELECT e FROM Employment e WHERE " +
        "e.personInvolved.id = :personId AND " +
        "e.organisationUnit IS NULL " +
        "ORDER BY e.id")
    List<Employment> findExternalByPersonInvolvedId(Integer personId);

    @Query("SELECT e FROM Employment e JOIN FETCH e.personInvolved pe " +
        "WHERE e.organisationUnit.id = :institutionId " +
        "ORDER BY e.id")
    List<Employment> findByEmploymentInstitutionId(Integer institutionId);

    @Query("SELECT e FROM Employment e JOIN FETCH e.personInvolved pe " +
        "WHERE e.dateTo IS NOT NULL AND " +
        "e.dateTo <= CURRENT_DATE " +
        "ORDER BY e.id")
    List<Employment> findEmploymentsExpiringToday();
}
