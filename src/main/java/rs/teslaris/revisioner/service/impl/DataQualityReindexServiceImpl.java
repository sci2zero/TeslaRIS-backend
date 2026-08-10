package rs.teslaris.revisioner.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rs.teslaris.core.applicationevent.DataQualityAssessmentReindexEvent;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.revisioner.indexrepository.DataQualityAssessmentIndexRepository;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.repository.DataQualityAssessmentRepository;
import rs.teslaris.revisioner.service.interfaces.DataQualityReindexService;
import rs.teslaris.revisioner.util.CompressionUtil;
import rs.teslaris.revisioner.util.ObjectMapperProvider;
import rs.teslaris.revisioner.util.RevisionHydratorRegistry;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentIndexer;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataQualityReindexServiceImpl implements DataQualityReindexService {

    private final DataQualityAssessmentRepository dataQualityAssessmentRepository;

    private final DataQualityAssessmentIndexRepository dataQualityAssessmentIndexRepository;

    private final DataQualityAssessmentIndexer dataQualityAssessmentIndexer;

    private final RevisionHydratorRegistry revisionHydratorRegistry;


    @Override
    public void reindexDataQualityAssessments() {
        dataQualityAssessmentIndexRepository.deleteAll();

        var objectMapper = ObjectMapperProvider.provideObjectmapper();

        // Ascending finishedAt order is load-bearing: DataQualityAssessmentIndexer chains
        // is_latest/superseded_at by looking up whichever doc is currently latest for the
        // same (entityType, entityId, profileName), so replaying assessments out of order
        // would rebuild a corrupted chain.
        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "finishedAt"),
            dataQualityAssessmentRepository::findAll,
            assessment -> reindexSingleAssessment(assessment, objectMapper)
        );

        log.info("Finished reindexing data quality assessments.");
    }

    private void reindexSingleAssessment(DataQualityAssessment assessment,
                                         ObjectMapper objectMapper) {
        var revision = assessment.getRevision();
        var entityType = revision.getEntityType();

        var targets = DataQualityAssessmentListener.resolveTargetTypes(entityType);
        if (targets.isEmpty()) {
            log.warn(
                "Unable to resolve target type for entityType={}, skipping assessment {}.",
                entityType, assessment.getId());
            return;
        }

        var dtoClass = revisionHydratorRegistry.getDtoClass(entityType);
        var json = CompressionUtil.decompress(revision.getCompressedContent());

        try {
            var dto = objectMapper.treeToValue(objectMapper.readTree(json), dtoClass);
            dataQualityAssessmentIndexer.index(assessment, targets, dto);
        } catch (JsonProcessingException e) {
            log.warn(
                "Failed to deserialize revision for assessment {} (entityType={}): {}",
                assessment.getId(), entityType, e.getMessage());
        }
    }

    @Async("taskExecutor")
    @EventListener
    @Override
    public void handleDataQualityAssessmentReindexEvent(DataQualityAssessmentReindexEvent event) {
        reindexDataQualityAssessments();
    }
}
