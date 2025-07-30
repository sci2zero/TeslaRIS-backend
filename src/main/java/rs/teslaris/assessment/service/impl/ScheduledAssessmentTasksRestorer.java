package rs.teslaris.assessment.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.ReportType;
import rs.teslaris.assessment.model.classification.EntityClassificationSource;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.service.interfaces.ReportingService;
import rs.teslaris.assessment.service.interfaces.classification.DocumentAssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.PublicationSeriesAssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.indicator.PublicationSeriesIndicatorService;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;
import rs.teslaris.core.service.impl.commontypes.ScheduledTasksRestorer;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduledAssessmentTasksRestorer {

    private final ReportingService reportingService;

    private final DocumentAssessmentClassificationService documentAssessmentClassificationService;

    private final PublicationSeriesAssessmentClassificationService
        publicationSeriesAssessmentClassificationService;

    private final PublicationSeriesIndicatorService publicationSeriesIndicatorService;

    private final ScheduledTaskMetadataRepository metadataRepository;

    private final TaskManagerService taskManagerService;

    private final ObjectMapper objectMapper;


    @EventListener(ApplicationReadyEvent.class)
    public void restoreTasksOnStartup() {
        List<ScheduledTaskMetadata> allMetadata = metadataRepository.findTasksByTypes(
            List.of(
                ScheduledTaskType.REPORT_GENERATION,
                ScheduledTaskType.PUBLICATION_CLASSIFICATION,
                ScheduledTaskType.JOURNAL_CLASSIFICATION,
                ScheduledTaskType.JOURNAL_CLASSIFICATION_LOADING,
                ScheduledTaskType.IF5_RANK_COMPUTATION,
                ScheduledTaskType.INDICATOR_LOADING
            ));

        for (ScheduledTaskMetadata metadata : allMetadata) {
            try {
                synchronized (ScheduledTasksRestorer.lock) {
                    restoreTaskFromMetadata(metadata);
                }
            } catch (Exception e) {
                log.error("Failed to restore assessment scheduled task: {}", metadata.getTaskId(),
                    e);
            }
        }
    }

    private void restoreTaskFromMetadata(ScheduledTaskMetadata metadata) {
        if (metadata.getType().equals(ScheduledTaskType.REPORT_GENERATION)) {
            restoreReportGeneration(metadata);
        } else if (metadata.getType().equals(ScheduledTaskType.PUBLICATION_CLASSIFICATION)) {
            restorePublicationClassification(metadata);
        } else if (metadata.getType().equals(ScheduledTaskType.JOURNAL_CLASSIFICATION)) {
            restoreJournalClassification(metadata);
        } else if (metadata.getType().equals(ScheduledTaskType.JOURNAL_CLASSIFICATION_LOADING)) {
            restoreJournalClassificationLoading(metadata);
        } else if (metadata.getType().equals(ScheduledTaskType.IF5_RANK_COMPUTATION)) {
            restoreIF5RankComputation(metadata);
        } else if (metadata.getType().equals(ScheduledTaskType.INDICATOR_LOADING)) {
            restoreIndicatorLoading(metadata);
        }

        metadataRepository.deleteTaskForTaskId(metadata.getTaskId());
    }

    private void restoreReportGeneration(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var reportType = ReportType.valueOf((String) data.get("reportType"));

        var assessmentYear = (Integer) data.get("assessmentYear");

        var commissionIds = objectMapper.convertValue(
            data.get("commissionIds"), new TypeReference<ArrayList<Integer>>() {
            }
        );

        var topLevelInstitutionId = (Integer) data.get("topLevelInstitutionId");

        var userId = (Integer) data.get("userId");
        var locale = (String) data.get("locale");

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        reportingService.scheduleReportGeneration(timeToRun, reportType, assessmentYear,
            commissionIds, locale, topLevelInstitutionId, userId);
    }

    private void restoreJournalClassification(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var commissionId = (Integer) data.get("commissionId");

        var classificationYears = objectMapper.convertValue(
            data.get("classificationYears"), new TypeReference<ArrayList<Integer>>() {
            }
        );

        var journalIds = objectMapper.convertValue(
            data.get("journalIds"), new TypeReference<ArrayList<Integer>>() {
            }
        );

        var userId = (Integer) data.get("userId");

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        publicationSeriesAssessmentClassificationService.scheduleClassification(timeToRun,
            commissionId, userId, classificationYears, journalIds);
    }

    private void restoreJournalClassificationLoading(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var source = EntityClassificationSource.valueOf((String) data.get("source"));
        var commissionId = (Integer) data.get("commissionId");
        var userId = (Integer) data.get("userId");

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        publicationSeriesAssessmentClassificationService.scheduleClassificationLoading(timeToRun,
            source, userId, commissionId);
    }

    private void restorePublicationClassification(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var userId = (Integer) data.get("userId");
        var fromDate = LocalDate.parse((String) data.get("fromDate"));
        var documentPublicationType =
            DocumentPublicationType.valueOf((String) data.get("documentPublicationType"));
        var commissionId = (Integer) data.get("commissionId");

        var authorIds = objectMapper.convertValue(
            data.get("authorIds"), new TypeReference<ArrayList<Integer>>() {
            }
        );

        var orgUnitIds = objectMapper.convertValue(
            data.get("orgUnitIds"), new TypeReference<ArrayList<Integer>>() {
            }
        );

        var publishedInIds = objectMapper.convertValue(
            data.get("publishedInIds"), new TypeReference<ArrayList<Integer>>() {
            }
        );

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        documentAssessmentClassificationService.schedulePublicationClassification(timeToRun, userId,
            fromDate, documentPublicationType, commissionId, authorIds, orgUnitIds, publishedInIds);
    }

    private void restoreIndicatorLoading(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var userId = (Integer) data.get("userId");
        var source =
            EntityIndicatorSource.valueOf((String) data.get("source"));

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        publicationSeriesIndicatorService.scheduleIndicatorLoading(timeToRun, source, userId);
    }

    private void restoreIF5RankComputation(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var userId = (Integer) data.get("userId");
        var classificationYears = objectMapper.convertValue(
            data.get("classificationYears"), new TypeReference<ArrayList<Integer>>() {
            }
        );

        var timeToRun = metadata.getTimeToRun();

        if (timeToRun.isBefore(LocalDateTime.now())) {
            timeToRun = taskManagerService.findNextFreeExecutionTime();
        }

        publicationSeriesIndicatorService.scheduleIF5RankComputation(timeToRun, classificationYears,
            userId);
    }
}
