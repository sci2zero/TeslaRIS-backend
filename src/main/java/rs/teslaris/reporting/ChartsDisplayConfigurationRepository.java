package rs.teslaris.reporting;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rs.teslaris.reporting.model.ChartsDisplayConfiguration;

public interface ChartsDisplayConfigurationRepository
    extends JpaRepository<ChartsDisplayConfiguration, Integer> {

    @Query("SELECT c FROM ChartsDisplayConfiguration c WHERE " +
        "c.organisationUnit.id = :institutionId")
    Optional<ChartsDisplayConfiguration> getConfigurationForInstitution(
        Integer institutionId);
}
