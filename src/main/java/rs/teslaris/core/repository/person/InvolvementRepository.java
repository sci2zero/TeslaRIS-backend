package rs.teslaris.core.repository.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Involvement;

@Repository
public interface InvolvementRepository extends JpaRepository<Involvement, Integer> {
}
