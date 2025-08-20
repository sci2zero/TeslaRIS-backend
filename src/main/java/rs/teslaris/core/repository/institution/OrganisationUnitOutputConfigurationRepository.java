package rs.teslaris.core.repository.institution;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.OrganisationUnitOutputConfiguration;

@Repository
public interface OrganisationUnitOutputConfigurationRepository
    extends JpaRepository<OrganisationUnitOutputConfiguration, Integer> {

    @Query("SELECT c FROM OrganisationUnitOutputConfiguration c WHERE " +
        "c.organisationUnit.id = :organisationUnitId")
    Optional<OrganisationUnitOutputConfiguration> findConfigurationForOrganisationUnit(
        Integer organisationUnitId);
}
