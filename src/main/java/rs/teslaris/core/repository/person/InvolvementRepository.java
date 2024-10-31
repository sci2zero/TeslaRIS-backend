package rs.teslaris.core.repository.person;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;

@Repository
public interface InvolvementRepository extends JpaRepository<Involvement, Integer> {

    @Query("select e from Employment e where e.personInvolved.id = :personId and e.organisationUnit.id = :institutionId and e.dateTo is null")
    Optional<Employment> findActiveEmploymentForPersonAndInstitution(Integer institutionId,
                                                                     Integer personId);
}
