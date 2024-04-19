package rs.teslaris.core.repository.person;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Employment;

@Repository
public interface EmploymentRepository extends JpaRepository<Employment, Integer> {

    @Query("select e from Employment e where e.personInvolved.id = :personId")
    List<Employment> findByPersonInvolvedId(Integer personId);
}
