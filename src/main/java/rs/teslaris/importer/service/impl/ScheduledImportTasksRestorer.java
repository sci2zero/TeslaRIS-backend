package rs.teslaris.importer.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import rs.teslaris.core.dto.commontypes.RelativeDateDTO;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;
import rs.teslaris.core.service.impl.commontypes.ScheduledTasksRestorer;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.importer.controller.CommonHarvestController;
import rs.teslaris.importer.service.interfaces.OAIPMHHarvester;
import rs.teslaris.importer.service.interfaces.SKGIFHarvester;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduledImportTasksRestorer {

    private final ScheduledTaskMetadataRepository metadataRepository;

    private final TaskManagerService taskManagerService;

    private final CommonHarvestController commonHarvestController;

    private final UserService userService;

    private final OAIPMHHarvester oaipmhHarvester;

    private final SKGIFHarvester skgifHarvester;

    private final ObjectMapper objectMapper;


    @EventListener(ApplicationReadyEvent.class)
    protected void restoreTasksOnStartup() {
        List<ScheduledTaskMetadata> allMetadata = metadataRepository.findTasksByTypes(
            List.of(
                ScheduledTaskType.PUBLICATION_HARVEST,
                ScheduledTaskType.AUTHOR_CENTRIC_PUBLICATION_HARVEST,
                ScheduledTaskType.OAI_PMH_HARVEST,
                ScheduledTaskType.SKG_IF_HARVEST
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
        } else if (metadata.getType().equals(ScheduledTaskType.OAI_PMH_HARVEST)) {
            restoreOAIPMHHarvest(metadata);
        } else if (metadata.getType().equals(ScheduledTaskType.SKG_IF_HARVEST)) {
            restoreSKGIFHarvest(metadata);
        }

        metadataRepository.deleteTaskForTaskId(metadata.getTaskId());
    }

    private void restorePublicationHarvest(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var userId = (Integer) data.get("userId");
        var userRole = (String) data.get("userRole");
        var dateFrom = RelativeDateDTO.parse((String) data.get("dateFrom"));
        var dateTo = RelativeDateDTO.parse((String) data.get("dateTo"));
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
            () -> commonHarvestController.performHarvest(userId, userRole, dateFrom.computeDate(),
                dateTo.computeDate(), institutionId),
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
        var dateFrom = RelativeDateDTO.parse((String) data.get("dateFrom"));
        var dateTo = RelativeDateDTO.parse((String) data.get("dateTo"));
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
                dateFrom.computeDate(), dateTo.computeDate(), authorIds, allAuthors, institutionId),
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

    private void restoreOAIPMHHarvest(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var userId = (Integer) data.get("userId");
        var sourceName = (String) data.get("sourceName");
        var from = RelativeDateDTO.parse((String) data.get("from"));
        var until = RelativeDateDTO.parse((String) data.get("until"));

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        var taskId = taskManagerService.scheduleTask(
            "OAIPMH_Harvest-" + sourceName +
                "-" + from + "_" + until +
                "-" + UUID.randomUUID(), timeToRun,
            () -> oaipmhHarvester.harvest(
                sourceName,
                from.computeDate(),
                until.computeDate(),
                userId),
            userId, metadata.getRecurrenceType());

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun,
                ScheduledTaskType.OAI_PMH_HARVEST, new HashMap<>() {{
                put("sourceName", sourceName);
                put("from", from.toString());
                put("until", until.toString());
                put("userId", userId);
            }}, metadata.getRecurrenceType()));
    }

    private void restoreSKGIFHarvest(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var userId = (Integer) data.get("userId");
        var sourceName = (String) data.get("sourceName");
        var from = RelativeDateDTO.parse((String) data.get("from"));
        var until = RelativeDateDTO.parse((String) data.get("until"));

        var authorIdentifier =
            StringUtil.normalizeNullString((String) data.get("authorIdentifier"));
        var institutionIdentifier =
            StringUtil.normalizeNullString((String) data.get("institutionIdentifier"));

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        var taskId = taskManagerService.scheduleTask(
            "SKGIF_Harvest-" + sourceName +
                "-" + from + "_" + until +
                "-" + UUID.randomUUID(), timeToRun,
            () -> skgifHarvester.harvest(
                sourceName, authorIdentifier,
                institutionIdentifier,
                from.computeDate(),
                until.computeDate(),
                userId
            ), userId, metadata.getRecurrenceType());

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun,
                ScheduledTaskType.SKG_IF_HARVEST, new HashMap<>() {{
                put("sourceName", sourceName);
                put("from", from.toString());
                put("until", until.toString());
                put("userId", userId);
                put("authorIdentifier", authorIdentifier);
                put("institutionIdentifier", institutionIdentifier);
            }}, metadata.getRecurrenceType()));
    }
}
