package rs.teslaris.migrator.pipeline;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.client.MigrationCursor;
import rs.teslaris.migrator.client.SourceBatch;
import rs.teslaris.migrator.model.MigrationItemStatus;
import rs.teslaris.migrator.model.MigrationRecordLog;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.MigrationRunStatus;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.repository.MigrationRunRepository;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * The migration algorithm, in one place.
 * <p>
 * Guarantees: a failing item never aborts the run, a failing batch never aborts the run, and every
 * outcome lands in both {@code migration.log} and the record log.
 */
@Component
@RequiredArgsConstructor
public class MigrationPipelineRunner {

    private final MigrationRunRepository runRepository;

    private final MigrationRecordLogRepository recordLogRepository;

    private final MigrationLog migrationLog;


    public <S> MigrationRun run(ResolvedPipeline<S> resolved, MigrationRun run) {
        var pipeline = resolved.pipeline();
        var cursor = new MigrationCursor(run.getCurrentPage(), null);

        migrationLog.runStarted(run);

        try {
            while (true) {
                SourceBatch<S> batch;

                try {
                    batch = fetchWithRetry(pipeline, cursor, run.getBatchSize());
                } catch (Exception e) {
                    // Log and continue with the next page - a bad batch must not end the run.
                    migrationLog.batchFailed(run, cursor, e);
                    run.setBatchesFailed(run.getBatchesFailed() + 1);
                    cursor = cursor.nextPage();
                    run.setCurrentPage(cursor.page());
                    runRepository.save(run);

                    if (run.getBatchesFailed() >= batchFailureLimit()) {
                        break;
                    }

                    continue;
                }

                if (batch.records().isEmpty()) {
                    break;
                }

                batch.records().forEach(record -> processRecord(resolved, run, record));

                run.setRecordsRead(run.getRecordsRead() + batch.records().size());
                cursor = batch.nextCursor();
                run.setCurrentPage(cursor.page());
                runRepository.save(run);

                if (!batch.hasMore()) {
                    break;
                }
            }

            run.setStatus(MigrationRunStatus.FINISHED);
        } catch (Exception e) {
            run.setStatus(MigrationRunStatus.FAILED);
            migrationLog.runAborted(run, e);
        }

        run.setFinishedAt(Instant.now());
        runRepository.save(run);
        migrationLog.runFinished(run);

        return run;
    }

    private <S> void processRecord(ResolvedPipeline<S> resolved, MigrationRun run, S record) {
        List<MigrationItem<?>> items;

        try {
            items = resolved.pipeline().router().route(record);
        } catch (Exception e) {
            migrationLog.recordRoutingFailed(run, recordId(resolved, record), e);
            run.setItemsFailed(run.getItemsFailed() + 1);
            return;
        }

        items.stream()
            .filter(resolved::accepts)
            .forEach(item -> processItem(resolved, run, item));
    }

    private <S> void processItem(ResolvedPipeline<S> resolved, MigrationRun run,
                                 MigrationItem<?> item) {
        if (recordLogRepository.isAlreadyProcessed(run.getSource(), item.type(),
            item.sourceKey())) {
            run.setItemsSkipped(run.getItemsSkipped() + 1);
            return;
        }

        try {
            var targetId = createWithRetry(resolved.pipeline(), run, item);
            run.setItemsCreated(run.getItemsCreated() + 1);
            logOutcome(run, item, MigrationItemStatus.CREATED, targetId, null);
        } catch (ResolvedByHandlerException e) {
            run.setItemsResolved(run.getItemsResolved() + 1);
            logOutcome(run, item, MigrationItemStatus.RESOLVED, e.resolvedEntityId(),
                e.getCause().getMessage());
        } catch (Exception e) {
            run.setItemsFailed(run.getItemsFailed() + 1);
            migrationLog.itemFailed(run, item, e);
            logOutcome(run, item, MigrationItemStatus.FAILED, null, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <S, D> Integer createWithRetry(MigrationPipeline<S> pipeline, MigrationRun run,
                                           MigrationItem<D> item) throws Exception {
        var retryPolicy = pipeline.retryPolicy();

        for (int attempt = 1; ; attempt++) {
            try {
                var targetId = item.create(run.isPerformIndex());
                migrationLog.itemCreated(run, item, targetId);
                return targetId;
            } catch (Exception e) {
                var resolution = item.handleFailure(e, attempt);

                switch (resolution.outcome()) {
                    case RESOLVED -> {
                        migrationLog.itemResolved(run, item, resolution.resolvedEntityId(), e);
                        throw new ResolvedByHandlerException(resolution.resolvedEntityId(), e);
                    }
                    case RETRY -> {
                        if (attempt >= retryPolicy.maxAttempts()) {
                            throw e;
                        }
                        sleep(retryPolicy.backoffFor(attempt));
                    }
                    default -> throw e;
                }
            }
        }
    }

    private <S> SourceBatch<S> fetchWithRetry(MigrationPipeline<S> pipeline, MigrationCursor cursor,
                                              int batchSize) throws Exception {
        var retryPolicy = pipeline.retryPolicy();
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            try {
                return pipeline.fetcher().fetch(cursor, batchSize);
            } catch (Exception e) {
                lastFailure = e;

                if (attempt < retryPolicy.maxAttempts()) {
                    sleep(retryPolicy.backoffFor(attempt));
                }
            }
        }

        throw Objects.requireNonNullElseGet(lastFailure,
            () -> new IllegalStateException("Batch fetch failed without an exception."));
    }

    private void logOutcome(MigrationRun run, MigrationItem<?> item, MigrationItemStatus status,
                            Integer targetId, String reason) {
        recordLogRepository.record(MigrationRecordLog.builder()
            .runId(run.getId())
            .source(run.getSource())
            .entityType(item.type())
            .sourceKey(item.sourceKey())
            .status(status)
            .targetEntityId(targetId)
            .reason(reason)
            .processedAt(Instant.now())
            .build());
    }

    private <S> String recordId(ResolvedPipeline<S> resolved, S record) {
        try {
            return resolved.pipeline().recordIdExtractor().apply(record);
        } catch (Exception e) {
            return "<unknown>";
        }
    }

    private int batchFailureLimit() {
        return 25;
    }

    private void sleep(java.time.Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            return;
        }

        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Internal signal that a {@link FailureHandler} dealt with the failure (merged, enriched), so the
     * item counts as resolved rather than failed.
     */
    private static class ResolvedByHandlerException extends RuntimeException {

        private final Integer resolvedEntityId;

        ResolvedByHandlerException(Integer resolvedEntityId, Exception cause) {
            super(cause);
            this.resolvedEntityId = resolvedEntityId;
        }

        Integer resolvedEntityId() {
            return resolvedEntityId;
        }
    }
}
