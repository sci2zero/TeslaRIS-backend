package rs.teslaris.core.repository.identifier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.identifier.DocumentIdentifier;

@Repository
public interface DocumentIdentifierRepository extends JpaRepository<DocumentIdentifier, Integer> {
}
