package rs.teslaris.revisioner.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.json.JsonData;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonSnapshotDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.revision.DataQualityFixator;
import rs.teslaris.core.revision.RevisionRestorer;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.jwt.TokenBasedAuthentication;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;
import rs.teslaris.revisioner.model.fixator.DataQualityFixatorPipeline;
import rs.teslaris.revisioner.repository.DataQualityFixatorPipelineRepository;
import rs.teslaris.revisioner.service.interfaces.DataQualityFixatorRunService;
import rs.teslaris.revisioner.service.interfaces.RevisionService;
import rs.teslaris.revisioner.util.ObjectMapperProvider;
import rs.teslaris.revisioner.util.RevisionRestorerRegistry;
import rs.teslaris.revisioner.util.fixators.DataQualityFixatorRegistry;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataQualityFixatorRunServiceImpl implements DataQualityFixatorRunService {

    private static final int FIX_SCAN_BATCH_SIZE = 1000;

    private static final String ASSESSMENT_INDEX_NAME = "data_quality_assessment";

    private static final List<String> FIX_SCAN_FIELDS = List.of("entity_type", "entity_id");

    private static final Set<String> RUNNING_PIPELINES = ConcurrentHashMap.newKeySet();

    private final DataQualityFixatorPipelineRepository dataQualityFixatorPipelineRepository;

    private final DataQualityFixatorRegistry dataQualityFixatorRegistry;

    private final RevisionRestorerRegistry revisionRestorerRegistry;

    private final RevisionService revisionService;

    private final SearchService<DataQualityAssessmentIndex> searchService;

    private final TaskManagerService taskManagerService;

    private final UserService userService;


    @Override
    public void scheduleFixatorRun(Instant timeToRun, @Nullable String pipelineName, Integer userId,
                                   RecurrenceType recurrence) {
        var taskId = taskManagerService.scheduleTask(
            "Data_Quality_Fix-" + Objects.requireNonNullElse(pipelineName, "ALL") +
                "-" + UUID.randomUUID(), timeToRun,
            Objects.isNull(pipelineName) ? () -> runAllPipelines(userId)
                : () -> runPipeline(pipelineName, userId),
            userId, recurrence);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun, ScheduledTaskType.DATA_QUALITY_FIX,
                new HashMap<>() {{
                    put("pipelineName", pipelineName);
                    put("userId", userId);
                }}, recurrence));
    }

    @Override
    public void runAllPipelines(Integer userId) {
        asUser(userId, () -> dataQualityFixatorPipelineRepository
            .findAllByOrderByPipelineNameAsc()
            .forEach(this::run));
    }

    @Override
    public void runPipeline(String pipelineName, Integer userId) {
        asUser(userId, () -> dataQualityFixatorPipelineRepository
            .findByPipelineName(pipelineName)
            .ifPresentOrElse(this::run,
                () -> log.warn("Fixator pipeline '{}' does not exist, nothing to run.",
                    pipelineName)));
    }

    /**
     * A run reads and writes whatever the user who asked for it can reach. A scheduled task has no
     * security context of its own, and without this an unapproved record is invisible to the run.
     */
    private void asUser(Integer userId, Runnable run) {
        var previousAuthentication = SecurityContextHolder.getContext().getAuthentication();

        try {
            SecurityContextHolder.getContext().setAuthentication(
                new TokenBasedAuthentication(userService.findOne(userId)));
            run.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }
    }

    private void run(DataQualityFixatorPipeline pipeline) {
        var pipelineName = pipeline.getPipelineName();

        if (Objects.isNull(pipeline.getMatchedRemarkKeys()) ||
            pipeline.getMatchedRemarkKeys().isEmpty()) {
            log.warn("Fixator pipeline '{}' matches no remark key, skipping it.", pipelineName);
            return;
        }

        var fixators = resolveFixators(pipeline);

        if (fixators.isEmpty()) {
            log.warn("Fixator pipeline '{}' applies no fixator, skipping it.", pipelineName);
            return;
        }

        if (!RUNNING_PIPELINES.add(pipelineName)) {
            log.warn("Fixator pipeline '{}' is already running, skipping this run.", pipelineName);
            return;
        }

        log.info("Running fixator pipeline '{}' over profile '{}'.", pipelineName,
            pipeline.getProfileName());

        var counters = new RunCounters();

        try {
            scan(pipeline, fixators, counters);
        } finally {
            RUNNING_PIPELINES.remove(pipelineName);
        }

        log.info("Fixator pipeline '{}' finished. Fixed {}, unchanged {}, skipped {}, failed {}.",
            pipelineName, counters.fixed, counters.unchanged, counters.skipped, counters.failed);
    }

    private List<DataQualityFixator> resolveFixators(DataQualityFixatorPipeline pipeline) {
        var fixators = new ArrayList<DataQualityFixator>();

        for (var key : Objects.requireNonNullElse(pipeline.getAppliedFixators(),
            List.<String>of())) {
            dataQualityFixatorRegistry.get(key).ifPresentOrElse(fixators::add,
                () -> log.warn("Pipeline '{}' applies unknown fixator '{}', skipping it.",
                    pipeline.getPipelineName(), key));
        }

        return fixators;
    }

    // The watermark includes its own id, because one entity id belongs to more than one entity
    // type.
    private void scan(DataQualityFixatorPipeline pipeline, List<DataQualityFixator> fixators,
                      RunCounters counters) {
        var pageable = PageRequest.of(0, FIX_SCAN_BATCH_SIZE,
            Sort.by(Sort.Direction.ASC, "entity_id"));
        var seenAtWatermark = new HashSet<String>();
        Integer watermark = null;

        while (true) {
            var batch = searchService.runQueryWithoutTotal(
                buildQuery(pipeline, watermark), pageable, DataQualityAssessmentIndex.class,
                ASSESSMENT_INDEX_NAME, FIX_SCAN_FIELDS).getContent();

            if (batch.isEmpty()) {
                break;
            }

            var records = new ArrayList<Pair<String, Integer>>();

            for (var assessment : batch) {
                if (Objects.isNull(assessment.getEntityType()) ||
                    Objects.isNull(assessment.getEntityId())) {
                    continue;
                }

                if (!Objects.equals(watermark, assessment.getEntityId())) {
                    watermark = assessment.getEntityId();
                    seenAtWatermark.clear();
                }

                // A fix reassesses the record and writes a new document under the same entity
                // id, thus the key is the record and not the document.
                if (seenAtWatermark.add(
                    assessment.getEntityType() + ":" + assessment.getEntityId())) {
                    records.add(
                        new Pair<>(assessment.getEntityType(), assessment.getEntityId()));
                }
            }

            // A fix mutates the index that the scan reads, thus the batch is fully read first.
            records.forEach(
                found -> fixRecord(pipeline, fixators, found.a, found.b, counters));

            if (batch.size() < FIX_SCAN_BATCH_SIZE) {
                break;
            }

            if (records.isEmpty()) {
                log.warn("More than {} assessments share entity id {}, skipping the remainder.",
                    FIX_SCAN_BATCH_SIZE, watermark);

                watermark = Objects.requireNonNullElse(watermark, 0) + 1;
                seenAtWatermark.clear();
            }
        }
    }

    private Query buildQuery(DataQualityFixatorPipeline pipeline, @Nullable Integer watermark) {
        return BoolQuery.of(b -> {
            b.must(TermQuery.of(t -> t.field("is_latest").value(true))._toQuery());
            b.must(TermQuery.of(
                t -> t.field("profile_name").value(pipeline.getProfileName()))._toQuery());
            b.must(TermsQuery.of(terms -> terms
                .field("failed_rule_keys")
                .terms(values -> values.value(
                    pipeline.getMatchedRemarkKeys().stream().map(FieldValue::of).toList()))
            )._toQuery());

            if (Objects.nonNull(watermark)) {
                b.must(RangeQuery.of(
                    r -> r.field("entity_id").gte(JsonData.of(watermark)))._toQuery());
            }

            return b;
        })._toQuery();
    }

    private void fixRecord(DataQualityFixatorPipeline pipeline, List<DataQualityFixator> fixators,
                           String entityType, Integer entityId, RunCounters counters) {
        try {
            var restorer = revisionRestorerRegistry.get(entityType).orElse(null);

            if (Objects.isNull(restorer)) {
                log.debug("No restorer for entity type '{}', skipping record {}.", entityType,
                    entityId);
                counters.skipped++;
                return;
            }

            if (Boolean.TRUE.equals(pipeline.getCreateVersionOnEveryStep())) {
                fixStepByStep(pipeline, fixators, restorer, entityType, entityId, counters);
                return;
            }

            fixInOneStep(pipeline, fixators, restorer, entityType, entityId, counters);
        } catch (Exception e) {
            log.warn("Pipeline '{}' failed on {} with ID {}. Reason: {}",
                pipeline.getPipelineName(), entityType, entityId, e.getMessage());
            counters.failed++;
        }
    }

    private void fixInOneStep(DataQualityFixatorPipeline pipeline,
                              List<DataQualityFixator> fixators, RevisionRestorer<?> restorer,
                              String entityType, Integer entityId, RunCounters counters) {
        var dto = restorer.readCurrentState(entityId);

        if (Objects.isNull(dto)) {
            log.debug("Current state of {} with ID {} is unreadable, skipping it.", entityType,
                entityId);
            counters.skipped++;
            return;
        }

        var before = serialise(dto);
        fixators.forEach(fixator -> dispatch(fixator, dto));

        if (before.equals(serialise(dto))) {
            counters.unchanged++;
            return;
        }

        write(pipeline, restorer, entityType, entityId, dto);
        counters.fixed++;
    }

    private void fixStepByStep(DataQualityFixatorPipeline pipeline,
                               List<DataQualityFixator> fixators, RevisionRestorer<?> restorer,
                               String entityType, Integer entityId, RunCounters counters) {
        var fixedAnything = false;

        for (var fixator : fixators) {
            var dto = restorer.readCurrentState(entityId);

            if (Objects.isNull(dto)) {
                log.debug("Current state of {} with ID {} is unreadable, skipping it.", entityType,
                    entityId);
                counters.skipped++;
                return;
            }

            var before = serialise(dto);
            dispatch(fixator, dto);

            if (before.equals(serialise(dto))) {
                continue;
            }

            write(pipeline, restorer, entityType, entityId, dto);
            fixedAnything = true;
        }

        if (fixedAnything) {
            counters.fixed++;
        } else {
            counters.unchanged++;
        }
    }

    private void write(DataQualityFixatorPipeline pipeline, RevisionRestorer<?> restorer,
                       String entityType, Integer entityId, Object dto) {
        // A journal is read as one DTO and written as its sibling.
        var writeDto = restorer.dtoClass().isInstance(dto) ? dto
            : ObjectMapperProvider.provideObjectmapper().convertValue(dto, restorer.dtoClass());

        revisionService.applyFixedState(entityType, entityId, writeDto,
            "revisionFixator:" + pipeline.getPipelineName());
    }

    private void dispatch(DataQualityFixator fixator, Object dto) {
        if (dto instanceof DocumentDTO document) {
            fixator.applyFix(document);
        } else if (dto instanceof PersonSnapshotDTO person) {
            fixator.applyFix(person);
        } else if (dto instanceof OrganisationUnitDTO organisationUnit) {
            fixator.applyFix(organisationUnit);
        } else if (dto instanceof EventDTO event) {
            fixator.applyFix(event);
        } else if (dto instanceof PublicationSeriesDTO publicationSeries) {
            fixator.applyFix(publicationSeries);
        } else if (dto instanceof PublisherDTO publisher) {
            fixator.applyFix(publisher);
        }
    }

    private String serialise(Object dto) {
        try {
            return ObjectMapperProvider.provideObjectmapper().writeValueAsString(dto);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialise the state of a fixed record.", e);
        }
    }

    private static final class RunCounters {

        private int fixed;

        private int unchanged;

        private int skipped;

        private int failed;
    }
}
