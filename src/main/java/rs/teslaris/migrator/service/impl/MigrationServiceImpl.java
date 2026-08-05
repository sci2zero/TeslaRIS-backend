package rs.teslaris.migrator.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.migrator.dto.MigrationFailureDTO;
import rs.teslaris.migrator.dto.MigrationRequestDTO;
import rs.teslaris.migrator.dto.MigrationRunResponseDTO;
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


    @Override
    public String startRun(MigrationRequestDTO request) {
        ResolvedPipeline<Object> resolved = pipelineRegistry
            .resolve(request.source(), request.entityType())
            .orElseThrow(() -> new NotFoundException(
                String.format("No migration pipeline registered for %s/%s.",
                    request.source(), request.entityType())));

        var run = createRun(request, resolved);

        runExecutor.execute(resolved, run);

        return run.getId();
    }

    @Override
    public MigrationRunResponseDTO getRun(String runId) {
        return MigrationRunResponseDTO.of(findRun(runId));
    }

    @Override
    public List<MigrationRunResponseDTO> listRuns(String source, MigrationEntityType entityType,
                                                  Pageable pageable) {
        return runRepository.findAll(source, entityType, pageable).stream()
            .map(MigrationRunResponseDTO::of)
            .toList();
    }

    @Override
    public List<MigrationFailureDTO> getFailures(String runId, Pageable pageable) {
        return recordLogRepository.findFailures(runId, pageable).stream()
            .map(MigrationFailureDTO::of)
            .toList();
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
            triggeredByUserId));
    }

    @Override
    public List<String> listRegisteredPipelines() {
        return pipelineRegistry.listRegistered();
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
