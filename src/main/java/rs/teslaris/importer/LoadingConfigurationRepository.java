package rs.teslaris.importer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.importer.model.configuration.LoadingConfiguration;

@Repository
public interface LoadingConfigurationRepository
    extends JpaRepository<LoadingConfiguration, Integer> {

    @Query("SELECT lc FROM LoadingConfiguration lc WHERE lc.institution.id = :institutionId")
    Optional<LoadingConfiguration> getLoadingConfigurationForInstitution(Integer institutionId);
}
