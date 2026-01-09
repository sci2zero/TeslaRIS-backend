package rs.teslaris.core.repository.commontypes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rs.teslaris.core.model.commontypes.ApplicationConfiguration;

public interface ApplicationConfigurationRepository
    extends JpaRepository<ApplicationConfiguration, Integer> {

    @Query("SELECT COUNT(c) > 0 FROM ApplicationConfiguration c WHERE c.isInMaintenanceMode = TRUE")
    boolean isApplicationInMaintenanceMode();
}
