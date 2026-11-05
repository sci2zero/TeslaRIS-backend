package rs.teslaris.migrator.service.impl;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;
import rs.teslaris.core.service.impl.commontypes.ScheduledTaskRestoration;
import rs.teslaris.core.service.impl.commontypes.ScheduledTasksRestorer;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.util.SchedulingTime;
import rs.teslaris.migrator.dto.MigrationScheduleRequestDTO;
import rs.teslaris.migrator.service.interfaces.MigrationService;
import rs.teslaris.migrator.util.MigrationEntityType;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledMigrationTasksRestorer {

    private final MigrationService migrationService;

    private final ScheduledTaskMetadataRepository metadataRepository;

    private final TaskManagerService taskManagerService;

    private final ScheduledTaskRestoration taskRestoration;


    @EventListener(ApplicationReadyEvent.class)
    protected void restoreTasksOnStartup() {
        List<ScheduledTaskMetadata> allMetadata = metadataRepository.findTasksByTypes(
            List.of(ScheduledTaskType.MIGRATION_SYNC));

        for (ScheduledTaskMetadata metadata : allMetadata) {
            try {
                synchronized (ScheduledTasksRestorer.lock) {
                    SchedulingTime.restoreInZone(
                        SchedulingTime.zoneFromMetadata(metadata.getMetadata()),
                        metadata.getTimeToRun(),
                        () -> taskRestoration.restoreOne(
                            () -> restoreTaskFromMetadata(metadata)));
                }
            } catch (Exception e) {
                log.error("Failed to restore migration scheduled task: {}", metadata.getTaskId(),
                    e);
            }
        }
    }

    private void restoreTaskFromMetadata(ScheduledTaskMetadata metadata) {
        var data = metadata.getMetadata();

        migrationService.scheduleRun(nextRunTime(metadata),
            new MigrationScheduleRequestDTO(
                (String) data.get("source"),
                MigrationEntityType.valueOf((String) data.get("entityType")),
                (Integer) data.get("batchSize"),
                Boolean.TRUE.equals(data.get("performIndex")),
                (Integer) data.get("modifiedAfterDays"),
                Boolean.TRUE.equals(data.get("backfillOnlyCreated")),
                Boolean.TRUE.equals(data.get("isUpdate")),
                (Integer) data.get("userId")), metadata.getRecurrenceType());

        metadataRepository.deleteTaskForTaskId(metadata.getTaskId());
    }

    private Instant nextRunTime(ScheduledTaskMetadata metadata) {
        var timeToRun = metadata.getTimeToRun();

        return timeToRun.isBefore(Instant.now()) ? taskManagerService.findNextFreeExecutionTime()
            : timeToRun;
    }
}
