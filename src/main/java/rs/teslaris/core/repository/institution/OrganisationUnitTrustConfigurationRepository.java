package rs.teslaris.core.repository.institution;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.OrganisationUnitTrustConfiguration;

@Repository
public interface OrganisationUnitTrustConfigurationRepository
    extends JpaRepository<OrganisationUnitTrustConfiguration, Integer> {

    @Query("SELECT c FROM OrganisationUnitTrustConfiguration c WHERE " +
        "c.organisationUnit.id = :organisationUnitId")
    Optional<OrganisationUnitTrustConfiguration> findConfigurationForOrganisationUnit(
        Integer organisationUnitId);
}
