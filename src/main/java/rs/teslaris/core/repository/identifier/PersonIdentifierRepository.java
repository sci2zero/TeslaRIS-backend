package rs.teslaris.core.repository.identifier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.identifier.PersonIdentifier;

@Repository
public interface PersonIdentifierRepository extends JpaRepository<PersonIdentifier, Integer> {
}
