package rs.teslaris.core.repository.identifier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.identifier.EntityIdentifier;

@Repository
public interface EntityIdentifierRepository extends JpaRepository<EntityIdentifier, Integer> {
}
