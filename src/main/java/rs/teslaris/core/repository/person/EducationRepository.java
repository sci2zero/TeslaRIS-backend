package rs.teslaris.core.repository.person;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface EducationRepository extends JPASoftDeleteRepository<Education> {
}
