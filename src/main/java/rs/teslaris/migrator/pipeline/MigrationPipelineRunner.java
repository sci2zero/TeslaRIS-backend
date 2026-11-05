package rs.teslaris.migrator.pipeline;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.client.MigrationCursor;
import rs.teslaris.migrator.client.SourceBatch;
import rs.teslaris.migrator.model.MigrationItemStatus;
import rs.teslaris.migrator.model.MigrationRecordLog;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.MigrationRunStatus;
import rs.teslaris.migrator.pipeline.merge.MergeContext;
import rs.teslaris.migrator.pipeline.merge.MigrationMergePolicy;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.repository.MigrationRunRepository;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * The migration algorithm, in one place.
 * <p>
 * Guarantees: a failing item never aborts the run, a failing batch does not abort the run until the
 * failures reach one of the configured limits, and every outcome lands in both
 * {@code migration.log} and the record log.
 */
@Component
@RequiredArgsConstructor
public class MigrationPipelineRunner {

    private final MigrationRunRepository runRepository;

    private final MigrationRecordLogRepository recordLogRepository;

    private final MigrationMergePolicy mergePolicy;

    private final MigrationLog migrationLog;

    @Value("${migrator.batch-failure-limit:25}")
    private int batchFailureLimit;

    @Value("${migrator.consecutive-batch-failure-limit:3}")
    private int consecutiveBatchFailureLimit;


    public <S> MigrationRun run(ResolvedPipeline<S> resolved, MigrationRun run) {
        var pipeline = resolved.pipeline();
        var cursor = new MigrationCursor(run.getCurrentPage(), null);
        var consecutiveBatchFailures = 0;
        String abandonReason = null;

        migrationLog.runStarted(run);

        try {
            while (true) {
                SourceBatch<S> batch;

                try {
                    batch = fetchWithRetry(pipeline, cursor, run.getBatchSize(), run);
                } catch (Exception e) {
                    // A bad page does not end the run, a source that keeps failing does.
                    migrationLog.batchFailed(run, cursor, e);
                    run.setBatchesFailed(run.getBatchesFailed() + 1);
                    consecutiveBatchFailures++;
                    cursor = cursor.nextPage();
                    run.setCurrentPage(cursor.page());
                    runRepository.save(run);

                    abandonReason = abandonReason(run, consecutiveBatchFailures);

                    if (Objects.nonNull(abandonReason)) {
                        break;
                    }

                    continue;
                }

                consecutiveBatchFailures = 0;

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

            if (Objects.nonNull(abandonReason)) {
                run.setStatus(MigrationRunStatus.FAILED);
                migrationLog.runAbandoned(run, abandonReason);
            } else {
                run.setStatus(MigrationRunStatus.FINISHED);
            }
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
        var decision = decide(run, item);

        switch (decision.action()) {
            case SKIP -> run.setItemsSkipped(run.getItemsSkipped() + 1);
            case CREATE -> createItem(resolved, run, item);
            case WRITTEN -> {
                if (run.isUpdate() && item.canUpdate() && Objects.nonNull(decision.targetId())) {
                    updateItem(resolved, run, item, decision);
                } else {
                    run.setItemsSkipped(run.getItemsSkipped() + 1);
                }
            }
        }
    }

    private Decision decide(MigrationRun run, MigrationItem<?> item) {
        var entry = recordLogRepository.find(run.getSource(), item.type(), item.sourceKey());

        if (entry.isPresent()) {
            var status = entry.get().getStatus();

            if (MigrationItemStatus.RESOLVED.equals(status)) {
                // TODO a later version can also update a RESOLVED item, which is an entity that
                //  TeslaRIS already owned before the migration matched it.
                return Decision.skip();
            }

            if (isWritten(status)) {
                return Decision.written(entry.get().getTargetEntityId(), status);
            }
        }

        return adopt(run, item);
    }

    /**
     * Finds the entity of an item that the record log does not hold under its current source key.
     */
    private Decision adopt(MigrationRun run, MigrationItem<?> item) {
        // The key format of a type can change, so a run first looks under the key of the earlier
        // version. Without this fallback the run would create a second entity for the same record.
        var adopted = legacyTargetId(run, item).or(() -> locate(item));

        if (adopted.isEmpty()) {
            return Decision.create();
        }

        // The current key gets an entry of its own, so the next run resolves it without a fallback.
        logOutcome(run, item, MigrationItemStatus.CREATED, adopted.get(),
            "adopted an existing entity");

        return Decision.written(adopted.get(), MigrationItemStatus.CREATED);
    }

    private Optional<Integer> legacyTargetId(MigrationRun run, MigrationItem<?> item) {
        if (Objects.isNull(item.legacyKey()) || item.legacyKey().isBlank() ||
            item.legacyKey().equals(item.sourceKey())) {
            return Optional.empty();
        }

        return recordLogRepository.find(run.getSource(), item.type(), item.legacyKey())
            .filter(entry -> isWritten(entry.getStatus()))
            .map(MigrationRecordLog::getTargetEntityId)
            .filter(Objects::nonNull);
    }

    private <D> Optional<Integer> locate(MigrationItem<D> item) {
        try {
            return item.updatePlan().locate(item.dto());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private boolean isWritten(MigrationItemStatus status) {
        return MigrationItemStatus.CREATED.equals(status) ||
            MigrationItemStatus.UPDATED.equals(status);
    }

    private <S> void createItem(ResolvedPipeline<S> resolved, MigrationRun run,
                                MigrationItem<?> item) {
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

    private <S, D> void updateItem(ResolvedPipeline<S> resolved, MigrationRun run,
                                   MigrationItem<D> item, Decision decision) {
        var targetId = decision.targetId();

        try {
            var merged = item.hasReader()
                ? mergePolicy.merge(item.dto(), item.readExisting(targetId),
                new MergeContext(run.getSource(), item.type().name(), item.sourceKey()))
                : item.dto();

            updateWithRetry(resolved.pipeline(), run, item, targetId, merged);

            run.setItemsUpdated(run.getItemsUpdated() + 1);
            logOutcome(run, item, MigrationItemStatus.UPDATED, targetId, null);
        } catch (Exception e) {
            run.setItemsFailed(run.getItemsFailed() + 1);
            migrationLog.itemFailed(run, item, e);

            // The entity exists, so its write status stays. A FAILED entry would let the next run
            // create a second entity for the same record.
            logOutcome(run, item, decision.previousStatus(), targetId,
                "update failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <S, D> Integer createWithRetry(MigrationPipeline<S> pipeline, MigrationRun run,
                                           MigrationItem<D> item) throws Exception {
        var retryPolicy = pipeline.retryPolicy();

        for (int attempt = 1; ; attempt++) {
            try {
                var targetId = item.create(run);
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

    /**
     * No failure handler runs here: a handler resolves to a different entity, which is right for a
     * create and wrong for an update.
     */
    private <S, D> void updateWithRetry(MigrationPipeline<S> pipeline, MigrationRun run,
                                        MigrationItem<D> item, Integer targetId, D merged)
        throws Exception {
        var retryPolicy = pipeline.retryPolicy();
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            try {
                item.update(targetId, merged, run);
                migrationLog.itemUpdated(run, item, targetId);
                return;
            } catch (Exception e) {
                lastFailure = e;

                if (attempt < retryPolicy.maxAttempts()) {
                    sleep(retryPolicy.backoffFor(attempt));
                }
            }
        }

        throw Objects.requireNonNullElseGet(lastFailure,
            () -> new IllegalStateException("Item update failed without an exception."));
    }

    private <S> SourceBatch<S> fetchWithRetry(MigrationPipeline<S> pipeline, MigrationCursor cursor,
                                              int batchSize, MigrationRun run) throws Exception {
        var retryPolicy = pipeline.retryPolicy();
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            try {
                return pipeline.fetcher().fetch(cursor, batchSize, run);
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

    /**
     * Why the run has to stop, or {@code null} while it may carry on.
     */
    private String abandonReason(MigrationRun run, int consecutiveBatchFailures) {
        if (consecutiveBatchFailures >= consecutiveBatchFailureLimit) {
            return consecutiveBatchFailures + " batches in a row could not be read, the source "
                + "looks unreachable";
        }

        if (run.getBatchesFailed() >= batchFailureLimit) {
            return run.getBatchesFailed() + " batches of this run could not be read";
        }

        return null;
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

    private enum Action {
        CREATE,
        WRITTEN,
        SKIP
    }

    private record Decision(Action action, Integer targetId, MigrationItemStatus previousStatus) {

        static Decision create() {
            return new Decision(Action.CREATE, null, null);
        }

        static Decision skip() {
            return new Decision(Action.SKIP, null, null);
        }

        static Decision written(Integer targetId, MigrationItemStatus previousStatus) {
            return new Decision(Action.WRITTEN, targetId, previousStatus);
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
