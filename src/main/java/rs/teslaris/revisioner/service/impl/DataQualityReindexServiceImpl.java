package rs.teslaris.revisioner.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import rs.teslaris.core.applicationevent.DataQualityAssessmentReindexEvent;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;
import rs.teslaris.revisioner.indexrepository.DataQualityAssessmentIndexRepository;
import rs.teslaris.revisioner.model.qualityassessment.AssessedEntityKey;
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

    /**
     * Entities per batch; each brings all of its assessments, so chains never span batches.
     */
    static final int ENTITY_BATCH_SIZE = 100;

    private final DataQualityAssessmentRepository dataQualityAssessmentRepository;

    private final DataQualityAssessmentIndexRepository dataQualityAssessmentIndexRepository;

    private final DataQualityAssessmentIndexer dataQualityAssessmentIndexer;

    private final RevisionHydratorRegistry revisionHydratorRegistry;

    private final PlatformTransactionManager transactionManager;


    @Override
    public void reindexDataQualityAssessments() {
        dataQualityAssessmentIndexRepository.deleteAll();

        var objectMapper = ObjectMapperProvider.provideObjectmapper();
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setReadOnly(true);

        var chain = new ChainState();
        var cursor = AssessedEntityKey.START;
        var indexed = 0;

        while (true) {
            var after = cursor;

            // revision content is a large object, which Postgres only streams inside a
            // transaction, so loading and decompressing run in one; Elasticsearch work does not.
            var batch = Objects.requireNonNull(
                transaction.execute(status -> loadBatch(after, objectMapper)));

            if (batch.lastKey().isEmpty()) {
                break;
            }

            var completed = new ArrayList<DataQualityAssessmentIndex>();
            for (var item : batch.items()) {
                var document = buildDocument(item);
                if (Objects.nonNull(document)) {
                    chain.append(document).ifPresent(completed::add);
                    indexed++;
                }
            }

            save(completed);
            cursor = batch.lastKey().get();
        }

        chain.finish().ifPresent(last -> save(List.of(last)));

        log.info("Finished reindexing data quality assessments. Indexed {} assessments.", indexed);
    }

    private PreparedBatch loadBatch(AssessedEntityKey after, ObjectMapper objectMapper) {
        var keys = dataQualityAssessmentRepository.findAssessedEntityKeysAfter(
            after.entityType(), after.entityId(), Limit.of(ENTITY_BATCH_SIZE));

        if (keys.isEmpty()) {
            return new PreparedBatch(List.of(), Optional.empty());
        }

        var first = keys.getFirst();
        var last = keys.getLast();
        var items = dataQualityAssessmentRepository.findAllWithRevisionForEntityRange(
                first.entityType(), first.entityId(), last.entityType(), last.entityId())
            .stream()
            .map(assessment -> prepare(assessment, objectMapper))
            .filter(Objects::nonNull)
            .toList();

        return new PreparedBatch(items, Optional.of(last));
    }

    private PreparedAssessment prepare(DataQualityAssessment assessment,
                                       ObjectMapper objectMapper) {
        try {
            var revision = assessment.getRevision();
            var entityType = revision.getEntityType();

            var targets = DataQualityAssessmentListener.resolveTargetTypes(entityType);
            if (Objects.isNull(targets) || targets.isEmpty()) {
                log.warn(
                    "Unable to resolve target type for entityType={}, skipping assessment {}.",
                    entityType, assessment.getId());
                return null;
            }

            // The indexer is fed the same projection the assessment was computed from, so a
            // reindex cannot resolve scope differently than the original run did.
            var dtoClass = revisionHydratorRegistry.getAssessmentDtoClass(entityType);
            var json = CompressionUtil.decompress(revision.getCompressedContent());
            var dto = objectMapper.treeToValue(objectMapper.readTree(json), dtoClass);

            return new PreparedAssessment(assessment, targets, dto);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize revision for assessment {}, skipping it: {}",
                assessment.getId(), e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to prepare assessment {} for reindexing, skipping it.",
                assessment.getId(), e);
        }

        return null;
    }

    private DataQualityAssessmentIndex buildDocument(PreparedAssessment item) {
        try {
            return dataQualityAssessmentIndexer.buildIndex(item.assessment(), item.targets(),
                item.dto());
        } catch (Exception e) {
            log.warn("Failed to build index document for assessment {}, skipping it.",
                item.assessment().getId(), e);
            return null;
        }
    }

    private void save(List<DataQualityAssessmentIndex> documents) {
        if (documents.isEmpty()) {
            return;
        }

        try {
            dataQualityAssessmentIndexRepository.saveAll(documents);
        } catch (Exception e) {
            log.error("Failed to save {} data quality assessment documents, first ID {}.",
                documents.size(), documents.getFirst().getId(), e);
        }
    }

    @EventListener
    @Override
    public void handleDataQualityAssessmentReindexEvent(DataQualityAssessmentReindexEvent event) {
        reindexDataQualityAssessments();
    }

    private record PreparedAssessment(DataQualityAssessment assessment, List<String> targets,
                                      Object dto) {
    }

    private record PreparedBatch(List<PreparedAssessment> items,
                                 Optional<AssessedEntityKey> lastKey) {
    }

    private record ChainKey(String entityType, Integer entityId, String profileName) {

        static ChainKey of(DataQualityAssessmentIndex document) {
            return new ChainKey(document.getEntityType(), document.getEntityId(),
                document.getProfileName());
        }
    }

    /**
     * Holds back the newest document of the current chain, since only its successor (or the end
     * of the chain) decides whether it is still the latest. Relies on chain-ordered input; chains
     * do not span batches today, but carrying the pending document across them keeps that an
     * optimisation rather than a requirement.
     */
    static final class ChainState {

        private DataQualityAssessmentIndex pending;

        /**
         * @return the previously pending document, now final, if there was one
         */
        Optional<DataQualityAssessmentIndex> append(
            DataQualityAssessmentIndex document) {
            var previous = pending;
            pending = document;

            if (Objects.isNull(previous)) {
                return Optional.empty();
            }

            if (ChainKey.of(previous).equals(ChainKey.of(document))) {
                previous.setValidTo(document.getAssessmentDate());
                previous.setLatest(false);
            }

            return Optional.of(previous);
        }

        Optional<DataQualityAssessmentIndex> finish() {
            var last = pending;
            pending = null;
            return Optional.ofNullable(last);
        }
    }
}
