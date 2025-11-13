package rs.teslaris.core.repository.person;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Employment;

@Repository
public interface EmploymentRepository extends JpaRepository<Employment, Integer> {

    @Query("SELECT e FROM Employment e WHERE e.personInvolved.id = :personId")
    List<Employment> findByPersonInvolvedId(Integer personId);

    @Query("SELECT e FROM Employment e WHERE e.organisationUnit.id = :institutionId")
    List<Employment> findByEmploymentInstitutionId(Integer institutionId);
}
