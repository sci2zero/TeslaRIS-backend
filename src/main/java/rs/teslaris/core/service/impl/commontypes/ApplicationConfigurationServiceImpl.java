package rs.teslaris.core.service.impl.commontypes;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MaintenanceModeDTO;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;
import rs.teslaris.core.model.commontypes.ApplicationConfiguration;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.repository.commontypes.ApplicationConfigurationRepository;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.ApplicationConfigurationService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.util.jwt.JwtUtil;

@Service
@RequiredArgsConstructor
public class ApplicationConfigurationServiceImpl extends JPAServiceImpl<ApplicationConfiguration>
    implements ApplicationConfigurationService {

    private final ApplicationConfigurationRepository applicationConfigurationRepository;

    private final TaskManagerService taskManagerService;

    private final ScheduledTaskMetadataRepository metadataRepository;

    private final JwtUtil jwtUtil;


    @Override
    protected JpaRepository<ApplicationConfiguration, Integer> getEntityRepository() {
        return applicationConfigurationRepository;
    }

    @Override
    public void scheduleMaintenanceMode(LocalDateTime startTime, String approximateEndMoment,
                                        Integer userId) {
        var taskId = taskManagerService.scheduleTask(
            "Maintenance-" + startTime + "-" + approximateEndMoment + "-" + UUID.randomUUID(),
            startTime,
            this::turnOnMaintenanceMode, userId, RecurrenceType.ONCE);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, startTime,
                ScheduledTaskType.MAINTENANCE, new HashMap<>() {{
                put("approximateEndMoment", approximateEndMoment);
                put("userId", userId);
            }}, RecurrenceType.ONCE));
    }

    @Override
    public void turnOnMaintenanceMode() {
        var config = getCurrentConfiguration();
        config.setIsInMaintenanceMode(true);

        jwtUtil.revokeAllNonAdminTokens();

        save(config);
    }

    @Override
    public void turnOffMaintenanceMode() {
        var config = getCurrentConfiguration();
        config.setIsInMaintenanceMode(false);

        save(config);
    }

    @Override
    public boolean isApplicationInMaintenanceMode() {
        return getCurrentConfiguration().getIsInMaintenanceMode();
    }

    @Override
    public MaintenanceModeDTO getNextScheduledMaintenance() {
        var maintenanceTasks = taskManagerService.listScheduledMaintenanceTasks();

        var firstToCome = maintenanceTasks.stream()
            .min(Comparator.comparing(ScheduledTaskResponseDTO::executionTime));

        if (firstToCome.isEmpty()) {
            return null;
        }

        var taskMetadata = metadataRepository.findTaskByTaskId(firstToCome.get().taskId());


        return taskMetadata.map(
                scheduledTaskMetadata ->
                    new MaintenanceModeDTO(scheduledTaskMetadata.getTimeToRun(),
                        scheduledTaskMetadata.getMetadata()
                            .getOrDefault("approximateEndMoment", "").toString()))
            .orElse(null);

    }

    private ApplicationConfiguration getCurrentConfiguration() {
        var allConfigs = applicationConfigurationRepository.findAll();
        if (allConfigs.size() > 1) {
            applicationConfigurationRepository.deleteAll();
            return new ApplicationConfiguration();
        } else if (allConfigs.isEmpty()) {
            return new ApplicationConfiguration();
        }

        return allConfigs.getFirst();
    }
}
