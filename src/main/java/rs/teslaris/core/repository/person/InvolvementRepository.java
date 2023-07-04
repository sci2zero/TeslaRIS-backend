package rs.teslaris.core.repository.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface InvolvementRepository extends JPASoftDeleteRepository<Involvement> {
}
