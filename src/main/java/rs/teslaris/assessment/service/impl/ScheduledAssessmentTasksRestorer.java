package rs.teslaris.assessment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.service.interfaces.ReportingService;
import rs.teslaris.assessment.service.interfaces.classification.DocumentAssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.PublicationSeriesAssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.indicator.PublicationSeriesIndicatorService;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledAssessmentTasksRestorer {

    private final ReportingService reportingService;

    private final DocumentAssessmentClassificationService documentAssessmentClassificationService;

    private final PublicationSeriesAssessmentClassificationService
        publicationSeriesAssessmentClassificationService;

    private final PublicationSeriesIndicatorService publicationSeriesIndicatorService;

    private final ScheduledTaskMetadataRepository metadataRepository;

    private final ObjectMapper objectMapper;


    @EventListener(ApplicationReadyEvent.class)
    public void restoreTasksOnStartup() {
        List<ScheduledTaskMetadata> allMetadata = metadataRepository.findAll();

        for (ScheduledTaskMetadata metadata : allMetadata) {
            try {
                restoreTaskFromMetadata(metadata);
            } catch (Exception e) {
                log.error("Failed to restore assessment scheduled task: {}", metadata.getTaskId(),
                    e);
            }
        }
    }

    private void restoreTaskFromMetadata(ScheduledTaskMetadata metadata) {
        if (metadata.getType().equals(ScheduledTaskType.REPORT_GENERATION)) {
            // TODO
        } else if (metadata.getType().equals(ScheduledTaskType.PUBLICATION_CLASSIFICATION)) {
            // TODO
        } else if (metadata.getType().equals(ScheduledTaskType.JOURNAL_CLASSIFICATION)) {
            // TODO
        } else if (metadata.getType().equals(ScheduledTaskType.JOURNAL_CLASSIFICATION_LOADING)) {
            // TODO
        } else if (metadata.getType().equals(ScheduledTaskType.IF5_RANK_COMPUTATION)) {
            // TODO
        } else if (metadata.getType().equals(ScheduledTaskType.INDICATOR_LOADING)) {
            // TODO
        }
    }
}
