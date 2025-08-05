package rs.teslaris.importer.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.importer.model.configuration.OrganisationUnitImportSourceConfiguration;

@Repository
public interface OrganisationUnitImportSourceConfigurationRepository
    extends JpaRepository<OrganisationUnitImportSourceConfiguration, Integer> {

    @Query("SELECT c FROM OrganisationUnitImportSourceConfiguration c WHERE " +
        "c.organisationUnit.id = :institutionId")
    Optional<OrganisationUnitImportSourceConfiguration> findConfigurationForInstitution(
        Integer institutionId);
}
