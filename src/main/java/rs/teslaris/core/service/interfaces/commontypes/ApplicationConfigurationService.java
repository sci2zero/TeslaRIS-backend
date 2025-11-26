package rs.teslaris.core.service.interfaces.commontypes;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MaintenanceInformationDTO;
import rs.teslaris.core.model.commontypes.ApplicationConfiguration;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface ApplicationConfigurationService extends JPAService<ApplicationConfiguration> {

    void scheduleMaintenanceMode(LocalDateTime startTime, String approximateEndMoment,
                                 Integer userId);

    void turnOnMaintenanceMode();

    void turnOffMaintenanceMode();

    boolean isApplicationInMaintenanceMode();

    MaintenanceInformationDTO getNextScheduledMaintenance();
}
