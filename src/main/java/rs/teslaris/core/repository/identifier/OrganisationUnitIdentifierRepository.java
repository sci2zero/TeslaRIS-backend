package rs.teslaris.core.repository.identifier;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.OrganisationUnitIdentifier;

@Repository
public interface OrganisationUnitIdentifierRepository
    extends JpaRepository<OrganisationUnitIdentifier, Integer> {

    @Query("SELECT oui FROM OrganisationUnitIdentifier oui " +
        "WHERE oui.organisationUnit.id = :organisationUnitId AND oui.identifier.accessLevel <= :accessLevel")
    List<OrganisationUnitIdentifier> findIdentifiersForOrganisationUnitAndIdentifierAccessLevel(
        Integer organisationUnitId,
        AccessLevel accessLevel);
}
