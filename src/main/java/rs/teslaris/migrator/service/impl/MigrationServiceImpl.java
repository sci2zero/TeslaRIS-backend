package rs.teslaris.migrator.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.migrator.configuration.MigrationSourceProperties;
import rs.teslaris.migrator.dto.MigrationFailureDTO;
import rs.teslaris.migrator.dto.MigrationPipelineResponseDTO;
import rs.teslaris.migrator.dto.MigrationRequestDTO;
import rs.teslaris.migrator.dto.MigrationRunResponseDTO;
import rs.teslaris.migrator.dto.MigrationScheduleRequestDTO;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.MigrationRunStatus;
import rs.teslaris.migrator.pipeline.MigrationPipelineRegistry;
import rs.teslaris.migrator.pipeline.ResolvedPipeline;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.repository.MigrationRunRepository;
import rs.teslaris.migrator.service.interfaces.MigrationService;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationServiceImpl implements MigrationService {

    private final MigrationPipelineRegistry pipelineRegistry;

    private final MigrationRunExecutor runExecutor;

    private final MigrationRunRepository runRepository;

    private final MigrationRecordLogRepository recordLogRepository;

    private final TaskManagerService taskManagerService;

    private final MigrationSourceProperties sourceProperties;


    @Override
    public String startRun(MigrationRequestDTO request) {
        var resolved = resolvePipeline(request.source(), request.entityType());
        var run = createRun(request, resolved);

        runExecutor.execute(resolved, run);

        return run.getId();
    }

    @Override
    public String runSynchronously(MigrationRequestDTO request) {
        var resolved = resolvePipeline(request.source(), request.entityType());
        var run = createRun(request, resolved);

        runExecutor.executeSynchronous(resolved, run);

        return run.getId();
    }

    @Override
    public String scheduleRun(Instant timeToRun, MigrationScheduleRequestDTO request,
                              RecurrenceType recurrence) {
        if (Objects.isNull(request.modifiedAfterDays()) || request.modifiedAfterDays() < 1) {
            throw new MigrationException(
                "The window of a scheduled migration must be one day or more.");
        }

        resolvePipeline(request.source(), request.entityType());

        var taskId = taskManagerService.scheduleTask(
            "Migration_Sync-" + request.source() + "-" + request.entityType() + "-" +
                UUID.randomUUID(), timeToRun,
            () -> runSynchronously(request.toRunRequest(modifiedAfterOf(request))),
            request.triggeredByUserId(), recurrence);

        try {
            taskManagerService.saveTaskMetadata(
                new ScheduledTaskMetadata(taskId, timeToRun,
                    ScheduledTaskType.MIGRATION_SYNC, new HashMap<>() {{
                    put("source", request.source());
                    put("entityType", request.entityType().name());
                    put("batchSize", request.batchSize());
                    put("performIndex", request.shouldPerformIndex());
                    put("modifiedAfterDays", request.modifiedAfterDays());
                    put("backfillOnlyCreated", request.shouldBackfillOnlyCreated());
                    put("isUpdate", request.shouldUpdate());
                    put("userId", request.triggeredByUserId());
                }}, recurrence));
        } catch (RuntimeException e) {
            // Without its row the task does not survive a restart, so it leaves the schedule.
            taskManagerService.dropScheduledTask(taskId);
            throw e;
        }

        return taskId;
    }

    @Override
    public MigrationRunResponseDTO getRun(String runId) {
        return MigrationRunResponseDTO.of(findRun(runId));
    }

    @Override
    public Page<MigrationRunResponseDTO> listRuns(String source, MigrationEntityType entityType,
                                                  Pageable pageable) {
        return runRepository.findAll(source, entityType, pageable)
            .map(MigrationRunResponseDTO::of);
    }

    @Override
    public Page<MigrationFailureDTO> getFailures(String runId, Pageable pageable) {
        return recordLogRepository.findFailures(runId, pageable).map(MigrationFailureDTO::of);
    }

    @Override
    public String retryFailed(String runId, Integer triggeredByUserId) {
        var previousRun = findRun(runId);

        if (MigrationRunStatus.RUNNING.equals(previousRun.getStatus())) {
            throw new MigrationException(
                "Cannot retry failures of a run that is still in progress.");
        }

        var removed = recordLogRepository.deleteFailures(runId);
        log.info("Cleared {} failed record log entries of run {}.", removed, runId);

        return startRun(new MigrationRequestDTO(
            previousRun.getSource(),
            previousRun.getEntityType(),
            previousRun.getBatchSize(),
            previousRun.isPerformIndex(),
            false,
            previousRun.getModifiedAfter(),
            previousRun.isBackfillOnlyCreated(),
            previousRun.isUpdate(),
            triggeredByUserId));
    }

    @Override
    public List<MigrationPipelineResponseDTO> listRegisteredPipelines() {
        return pipelineRegistry.listPipelines().stream()
            .map(pipeline -> MigrationPipelineResponseDTO.of(pipeline,
                runExecutor.isRunning(pipeline.sourceName(), pipeline.entityType().name())))
            .toList();
    }

    private ResolvedPipeline<Object> resolvePipeline(String source,
                                                     MigrationEntityType entityType) {
        return pipelineRegistry.resolve(source, entityType)
            .orElseThrow(() -> new NotFoundException(
                String.format("No migration pipeline registered for %s/%s.", source, entityType)));
    }

    private String modifiedAfterOf(MigrationScheduleRequestDTO request) {
        return sourceProperties.forSource(request.source()).formatModifiedAfter(
            Instant.now().minus(request.modifiedAfterDays(), ChronoUnit.DAYS));
    }

    private <S> MigrationRun createRun(MigrationRequestDTO request, ResolvedPipeline<S> resolved) {
        var batchSize = Objects.requireNonNullElse(
            request.batchSize(), resolved.pipeline().defaultBatchSize());

        var startPage = request.shouldResume() ? lastReachedPage(request) : 0;

        return runRepository.save(MigrationRun.builder()
            .source(request.source())
            .entityType(request.entityType())
            .status(MigrationRunStatus.RUNNING)
            .startedAt(Instant.now())
            .currentPage(startPage)
            .batchSize(batchSize)
            .performIndex(request.shouldPerformIndex())
            .modifiedAfter(request.modifiedAfter())
            .backfillOnlyCreated(request.shouldBackfillOnlyCreated())
            .isUpdate(request.shouldUpdate())
            .triggeredByUserId(request.triggeredByUserId())
            .build());
    }

    private int lastReachedPage(MigrationRequestDTO request) {
        return runRepository
            .findAll(request.source(), request.entityType(), Pageable.ofSize(1)).stream()
            .findFirst()
            .map(MigrationRun::getCurrentPage)
            .orElse(0);
    }

    private MigrationRun findRun(String runId) {
        return runRepository.findById(runId)
            .orElseThrow(() -> new NotFoundException(
                String.format("Migration run %s does not exist.", runId)));
    }
}
