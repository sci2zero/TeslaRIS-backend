package rs.teslaris.core.repository.person;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;

@Repository
public interface InvolvementRepository extends JpaRepository<Involvement, Integer> {

    @Query("SELECT e FROM Employment e " +
        "WHERE e.personInvolved.id = :personId " +
        "AND e.organisationUnit.id = :institutionId " +
        "AND e.dateTo IS null")
    Optional<Employment> findActiveEmploymentForPersonAndInstitution(Integer institutionId,
                                                                     Integer personId);

    @Query("SELECT e FROM Employment e LEFT JOIN FETCH e.organisationUnit " +
        "WHERE e.personInvolved.id = :personId " +
        "AND e.organisationUnit.id IN :institutionIds " +
        "AND e.dateTo IS null")
    List<Employment> findActiveEmploymentsForPersonAndInstitutions(List<Integer> institutionIds,
                                                                   Integer personId);
}
