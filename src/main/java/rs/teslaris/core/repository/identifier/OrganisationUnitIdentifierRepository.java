package rs.teslaris.core.repository.identifier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.identifier.OrganisationUnitIdentifier;

@Repository
public interface OrganisationUnitIdentifierRepository
    extends JpaRepository<OrganisationUnitIdentifier, Integer> {
}
