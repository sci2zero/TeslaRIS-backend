package rs.teslaris.revisioner.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import rs.teslaris.core.util.SchedulingTime;
import rs.teslaris.revisioner.model.QualityAssessmentTarget;
import rs.teslaris.revisioner.service.interfaces.DataQualityFixatorRunService;
import rs.teslaris.revisioner.service.interfaces.QualityAssessmentBackfillService;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduledRevisionerTasksRestorer {

    private final QualityAssessmentBackfillService qualityAssessmentBackfillService;

    private final DataQualityFixatorRunService dataQualityFixatorRunService;

    private final ScheduledTaskMetadataRepository metadataRepository;

    private final TaskManagerService taskManagerService;

    private final ObjectMapper objectMapper;


    @EventListener(ApplicationReadyEvent.class)
    protected void restoreTasksOnStartup() {
        List<ScheduledTaskMetadata> allMetadata = metadataRepository.findTasksByTypes(
            List.of(
                ScheduledTaskType.QUALITY_ASSESSMENT_BACKFILL,
                ScheduledTaskType.DATA_QUALITY_FIX
            ));

        for (ScheduledTaskMetadata metadata : allMetadata) {
            try {
                synchronized (ScheduledTasksRestorer.lock) {
                    SchedulingTime.restoreInZone(
                        SchedulingTime.zoneFromMetadata(metadata.getMetadata()),
                        metadata.getTimeToRun(),
                        () -> restoreTaskFromMetadata(metadata));
                }
            } catch (Exception e) {
                log.error("Failed to restore revisioner scheduled task: {}", metadata.getTaskId(),
                    e);
            }
        }
    }

    private void restoreTaskFromMetadata(ScheduledTaskMetadata metadata) {
        if (metadata.getType().equals(ScheduledTaskType.QUALITY_ASSESSMENT_BACKFILL)) {
            restoreQualityAssessmentBackfill(metadata);
        } else if (metadata.getType().equals(ScheduledTaskType.DATA_QUALITY_FIX)) {
            restoreDataQualityFix(metadata);
        }

        metadataRepository.deleteTaskForTaskId(metadata.getTaskId());
    }

    private void restoreQualityAssessmentBackfill(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var targets = objectMapper.convertValue(
            data.get("entityTypes"), new TypeReference<ArrayList<QualityAssessmentTarget>>() {
            }
        );

        var personIds = objectMapper.convertValue(
            data.get("personIds"), new TypeReference<ArrayList<Integer>>() {
            }
        );

        var organisationUnitIds = objectMapper.convertValue(
            data.get("organisationUnitIds"), new TypeReference<ArrayList<Integer>>() {
            }
        );

        var profileName = (String) data.get("profileName");
        var rewriteExistingAssessments =
            Boolean.TRUE.equals(data.get("rewriteExistingAssessments"));
        var userId = (Integer) data.get("userId");

        qualityAssessmentBackfillService.scheduleBackfill(nextRunTime(metadata), targets, personIds,
            organisationUnitIds, profileName, rewriteExistingAssessments, userId,
            metadata.getRecurrenceType());
    }

    private void restoreDataQualityFix(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var pipelineName = (String) data.get("pipelineName");
        var userId = (Integer) data.get("userId");

        dataQualityFixatorRunService.scheduleFixatorRun(nextRunTime(metadata), pipelineName, userId,
            metadata.getRecurrenceType());
    }

    private Instant nextRunTime(ScheduledTaskMetadata metadata) {
        var timeToRun = metadata.getTimeToRun();

        return timeToRun.isBefore(Instant.now()) ? taskManagerService.findNextFreeExecutionTime()
            : timeToRun;
    }
}
