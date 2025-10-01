package rs.teslaris.core.service.impl.commontypes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.document.DocumentFileSection;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;
import rs.teslaris.core.service.interfaces.commontypes.ReindexService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.DocumentBackupService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.ThesisService;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduledTasksRestorer {

    public static final Object lock = new Object();

    private final DocumentBackupService documentBackupService;

    private final ReindexService reindexService;

    private final ScheduledTaskMetadataRepository metadataRepository;

    private final TaskManagerService taskManagerService;

    private final DocumentPublicationService documentPublicationService;

    private final ThesisService thesisService;

    private final ObjectMapper objectMapper;


    @EventListener(ApplicationReadyEvent.class)
    public void restoreTasksOnStartup() {
        List<ScheduledTaskMetadata> allMetadata = metadataRepository.findTasksByTypes(
            List.of(
                ScheduledTaskType.DOCUMENT_BACKUP,
                ScheduledTaskType.REINDEXING,
                ScheduledTaskType.UNMANAGED_DOCUMENTS_DELETION,
                ScheduledTaskType.PUBLIC_REVIEW_END_DATE_CHECK
            ));

        for (ScheduledTaskMetadata metadata : allMetadata) {
            try {
                synchronized (lock) {
                    restoreTaskFromMetadata(metadata);
                }
            } catch (Exception e) {
                log.error("Failed to restore scheduled task: {}", metadata.getTaskId(), e);
            }
        }
    }

    private void restoreTaskFromMetadata(ScheduledTaskMetadata metadata) {
        if (metadata.getType().equals(ScheduledTaskType.DOCUMENT_BACKUP)) {
            restoreDocumentBackup(metadata);
        } else if (metadata.getType().equals(ScheduledTaskType.REINDEXING)) {
            restoreReindexOperation(metadata);
        } else if (metadata.getType().equals(ScheduledTaskType.UNMANAGED_DOCUMENTS_DELETION)) {
            restoreUnmanagedDocumentsDeletion(metadata);
        } else if (metadata.getType().equals(ScheduledTaskType.PUBLIC_REVIEW_END_DATE_CHECK)) {
            restorePublicReviewEndDateCheck(metadata);
        }

        metadataRepository.deleteTaskForTaskId(metadata.getTaskId());
    }

    private void restoreDocumentBackup(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var institutionId = (Integer) data.get("institutionId");
        var from = Integer.parseInt((String) data.get("from"));
        var to = Integer.parseInt((String) data.get("to"));

        var fileSections = objectMapper.convertValue(
            data.get("documentFileSections"), new TypeReference<ArrayList<DocumentFileSection>>() {
            }
        );

        var types = objectMapper.convertValue(
            data.get("types"), new TypeReference<ArrayList<DocumentPublicationType>>() {
            }
        );

        var language = (String) data.get("language");
        var userId = (Integer) data.get("userId");
        var metadataFormat = ExportFileType.valueOf((String) data.get("metadataFormat"));

        documentBackupService.scheduleBackupGeneration(institutionId, from, to, types,
            fileSections, userId, language, metadataFormat, metadata.getRecurrenceType());
    }

    private void restoreReindexOperation(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var indexesToRepopulate = objectMapper.convertValue(
            data.get("indexesToRepopulate"), new TypeReference<ArrayList<EntityType>>() {
            }
        );

        var userId = (Integer) data.get("userId");

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        var taskId = taskManagerService.scheduleTask(
            "DatabaseReindex-" +
                StringUtils.join(indexesToRepopulate.stream().map(
                    EntityType::name).toList(), "-") +
                "-" + UUID.randomUUID(),
            timeToRun,
            () -> reindexService.reindexDatabase(indexesToRepopulate),
            userId, RecurrenceType.ONCE);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun,
                ScheduledTaskType.REINDEXING, new HashMap<>() {{
                put("indexesToRepopulate", indexesToRepopulate);
                put("userId", userId);
            }}, RecurrenceType.ONCE));
    }

    private void restoreUnmanagedDocumentsDeletion(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var userId = (Integer) data.get("userId");

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        var taskId = taskManagerService.scheduleTask(
            "Unmanaged_Documents_Deletion-" +
                "-" + UUID.randomUUID(), timeToRun,
            documentPublicationService::deleteNonManagedDocuments,
            userId, metadata.getRecurrenceType());

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun,
                ScheduledTaskType.UNMANAGED_DOCUMENTS_DELETION, new HashMap<>(),
                metadata.getRecurrenceType()));
    }

    private void restorePublicReviewEndDateCheck(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var userId = (Integer) data.get("userId");
        var publicReviewLengthDays = (Integer) data.get("publicReviewLengthDays");
        var types = objectMapper.convertValue(
            data.get("types"), new TypeReference<ArrayList<ThesisType>>() {
            }
        );

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        thesisService.schedulePublicReviewEndCheck(timeToRun, types, publicReviewLengthDays, userId,
            metadata.getRecurrenceType());
    }
}

