package rs.teslaris.assessment.repository.indicator;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.indicator.ExternalIndicatorConfiguration;

@Repository
public interface ExternalIndicatorConfigurationRepository
    extends JpaRepository<ExternalIndicatorConfiguration, Integer> {

    @Query("SELECT eic FROM ExternalIndicatorConfiguration eic " +
        "WHERE eic.institution.id = :institutionId")
    Optional<ExternalIndicatorConfiguration> findByInstitutionId(Integer institutionId);
}
