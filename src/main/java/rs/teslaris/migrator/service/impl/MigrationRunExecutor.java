package rs.teslaris.migrator.service.impl;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.MigrationRunStatus;
import rs.teslaris.migrator.pipeline.MigrationPipelineRunner;
import rs.teslaris.migrator.pipeline.ResolvedPipeline;
import rs.teslaris.migrator.repository.MigrationRunRepository;

/**
 * Runs migrations off the request thread, one at a time per (source, entity type).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MigrationRunExecutor {

    private final MigrationPipelineRunner pipelineRunner;

    private final MigrationRunRepository runRepository;

    private final Map<String, ReentrantLock> runLocks = new ConcurrentHashMap<>();


    @Async("migrationExecutor")
    public <S> void execute(ResolvedPipeline<S> resolved, MigrationRun run) {
        var lock = runLocks.computeIfAbsent(
            run.getSource() + "#" + run.getEntityType(), key -> new ReentrantLock());

        if (!lock.tryLock()) {
            log.warn("Migration of {}/{} already in progress, abandoning run {}.",
                run.getSource(), run.getEntityType(), run.getId());

            run.setStatus(MigrationRunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            runRepository.save(run);
            return;
        }

        try {
            pipelineRunner.run(resolved, run);
        } catch (Exception e) {
            log.error("Migration run {} failed unexpectedly.", run.getId(), e);

            run.setStatus(MigrationRunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            runRepository.save(run);
        } finally {
            lock.unlock();
        }
    }

    public boolean isRunning(String source, String entityType) {
        var lock = runLocks.get(source + "#" + entityType);
        return Objects.nonNull(lock) && lock.isLocked();
    }
}
