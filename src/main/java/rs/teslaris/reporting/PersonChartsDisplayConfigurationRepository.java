package rs.teslaris.reporting;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rs.teslaris.reporting.model.PersonChartsDisplayConfiguration;

public interface PersonChartsDisplayConfigurationRepository
    extends JpaRepository<PersonChartsDisplayConfiguration, Integer> {

    @Query("SELECT c FROM PersonChartsDisplayConfiguration c WHERE " +
        "c.organisationUnit.id = :institutionId")
    Optional<PersonChartsDisplayConfiguration> getConfigurationForInstitution(
        Integer institutionId);
}
