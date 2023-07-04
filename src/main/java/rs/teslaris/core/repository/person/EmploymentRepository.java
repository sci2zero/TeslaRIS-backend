package rs.teslaris.core.repository.person;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface EmploymentRepository extends JPASoftDeleteRepository<Employment> {
}
