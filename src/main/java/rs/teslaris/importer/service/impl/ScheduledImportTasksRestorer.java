package rs.teslaris.importer.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;
import rs.teslaris.core.service.impl.commontypes.ScheduledTasksRestorer;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.importer.controller.CommonHarvestController;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduledImportTasksRestorer {

    private final ScheduledTaskMetadataRepository metadataRepository;

    private final TaskManagerService taskManagerService;

    private final CommonHarvestController commonHarvestController;

    private final UserService userService;

    private final ObjectMapper objectMapper;


    @EventListener(ApplicationReadyEvent.class)
    public void restoreTasksOnStartup() {
        List<ScheduledTaskMetadata> allMetadata = metadataRepository.findTasksByTypes(
            List.of(
                ScheduledTaskType.DOCUMENT_BACKUP,
                ScheduledTaskType.REINDEXING
            ));

        for (ScheduledTaskMetadata metadata : allMetadata) {
            try {
                synchronized (ScheduledTasksRestorer.lock) {
                    restoreTaskFromMetadata(metadata);
                }
            } catch (Exception e) {
                log.error("Failed to restore scheduled import: {}", metadata.getTaskId(), e);
            }
        }
    }

    private void restoreTaskFromMetadata(ScheduledTaskMetadata metadata) {
        if (metadata.getType().equals(ScheduledTaskType.PUBLICATION_HARVEST)) {
            restorePublicationHarvest(metadata);
        } else if (metadata.getType()
            .equals(ScheduledTaskType.AUTHOR_CENTRIC_PUBLICATION_HARVEST)) {
            restoreAuthorCentricPublicationHarvest(metadata);
        }

        metadataRepository.deleteTaskForTaskId(metadata.getTaskId());
    }

    private void restorePublicationHarvest(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var userId = (Integer) data.get("userId");
        var userRole = (String) data.get("userRole");
        var dateFrom = LocalDate.parse((String) data.get("dateFrom"));
        var dateTo = LocalDate.parse((String) data.get("dateTo"));
        var institutionId = (Integer) data.get("institutionId");

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        var taskId = taskManagerService.scheduleTask(
            "Harvest-" + ((Objects.nonNull(institutionId) && institutionId > 0) ? institutionId :
                userService.getUserOrganisationUnitId(userId)) +
                "-" + dateFrom + "_" + dateTo +
                "-" + UUID.randomUUID(), timeToRun,
            () -> commonHarvestController.performHarvest(userId, userRole, dateFrom, dateTo,
                institutionId),
            userId, metadata.getRecurrenceType());

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun,
                ScheduledTaskType.PUBLICATION_HARVEST, new HashMap<>() {{
                put("userRole", userRole);
                put("dateFrom", dateFrom.toString());
                put("dateTo", dateTo.toString());
                put("userId", userId);
                put("institutionId", institutionId);
            }}, metadata.getRecurrenceType()));
    }

    private void restoreAuthorCentricPublicationHarvest(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var userId = (Integer) data.get("userId");
        var userRole = (String) data.get("userRole");
        var dateFrom = LocalDate.parse((String) data.get("dateFrom"));
        var dateTo = LocalDate.parse((String) data.get("dateTo"));
        var institutionId = (Integer) data.get("institutionId");
        var allAuthors = (Boolean) data.get("allAuthors");

        var authorIds = objectMapper.convertValue(
            data.get("authorIds"), new TypeReference<ArrayList<Integer>>() {
            }
        );

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        var taskId = taskManagerService.scheduleTask(
            "Harvest-" + ((Objects.nonNull(institutionId) && institutionId > 0) ? institutionId :
                userService.getUserOrganisationUnitId(userId)) +
                "-" + dateFrom + "_" + dateTo +
                "-" + UUID.randomUUID(), timeToRun,
            () -> commonHarvestController.performAuthorCentricLoading(userId, userRole,
                dateFrom, dateTo, authorIds, allAuthors, institutionId),
            userId, metadata.getRecurrenceType());

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun,
                ScheduledTaskType.AUTHOR_CENTRIC_PUBLICATION_HARVEST, new HashMap<>() {{
                put("userRole", userRole);
                put("dateFrom", dateFrom.toString());
                put("dateTo", dateTo.toString());
                put("authorIds", authorIds);
                put("allAuthors", allAuthors);
                put("userId", userId);
                put("institutionId", institutionId);
            }}, metadata.getRecurrenceType()));
    }

}
