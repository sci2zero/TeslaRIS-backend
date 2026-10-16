package rs.teslaris.migrator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.client.MigrationCursor;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.pipeline.MigrationItem;

/**
 * Writes to the dedicated {@code MIGRATION} logger, which {@code logback-spring.xml} binds to
 * {@code application-logs/migration.log}. One structured, greppable line per event.
 */
@Component
public class MigrationLog {

    private static final Logger log = LoggerFactory.getLogger("MIGRATION");


    public void runStarted(MigrationRun run) {
        log.info("runId={} | source={} | entity={} | status=RUN_STARTED | batchSize={} | " +
                "performIndex={} | modifiedAfter={}",
            run.getId(), run.getSource(), run.getEntityType(), run.getBatchSize(),
            run.isPerformIndex(), run.getModifiedAfter());
    }

    public void runFinished(MigrationRun run) {
        log.info(
            "runId={} | source={} | entity={} | status=RUN_FINISHED | read={} | created={} | " +
                "resolved={} | skipped={} | failed={} | batchesFailed={}",
            run.getId(), run.getSource(), run.getEntityType(), run.getRecordsRead(),
            run.getItemsCreated(), run.getItemsResolved(), run.getItemsSkipped(),
            run.getItemsFailed(), run.getBatchesFailed());
    }

    public void runAborted(MigrationRun run, Exception exception) {
        log.error("runId={} | source={} | entity={} | status=RUN_FAILED | reason={}",
            run.getId(), run.getSource(), run.getEntityType(), exception.getMessage(), exception);
    }

    public void batchFailed(MigrationRun run, MigrationCursor cursor, Exception exception) {
        log.warn("runId={} | source={} | entity={} | page={} | status=BATCH_FAILED | reason={}",
            run.getId(), run.getSource(), run.getEntityType(), cursor.page(),
            exception.getMessage(), exception);
    }

    public void recordRoutingFailed(MigrationRun run, String recordId, Exception exception) {
        log.warn(
            "runId={} | source={} | entity={} | key={} | status=RECORD_UNCONVERTIBLE | reason={}",
            run.getId(), run.getSource(), run.getEntityType(), recordId, exception.getMessage(),
            exception);
    }

    public void itemCreated(MigrationRun run, MigrationItem<?> item, Integer targetId) {
        log.info("runId={} | source={} | entity={} | key={} | status=CREATED | targetId={}",
            run.getId(), run.getSource(), item.type(), item.sourceKey(), targetId);
    }

    public void itemResolved(MigrationRun run, MigrationItem<?> item, Integer targetId,
                             Exception exception) {
        log.info("runId={} | source={} | entity={} | key={} | status=RESOLVED | targetId={} | " +
                "reason={}",
            run.getId(), run.getSource(), item.type(), item.sourceKey(), targetId,
            exception.getMessage());
    }

    /**
     * A converter could not carry a source value over to the target; the item itself proceeds.
     */
    public void valueDropped(String source, String entity, String key, String rule,
                             String reason) {
        log.info("source={} | entity={} | key={} | status=VALUE_DROPPED | rule={} | reason={}",
            source, entity, key, rule, reason);
    }

    /**
     * A referenced entity the source names but does not describe (a journal of an article) was
     * created as a placeholder, to be merged with real data later.
     */
    public void stubCreated(String source, String entity, String key, Integer targetId,
                            String label) {
        log.info("source={} | entity={} | key={} | status=STUB_CREATED | targetId={} | label={}",
            source, entity, key, targetId, label);
    }

    public void itemFailed(MigrationRun run, MigrationItem<?> item, Exception exception) {
        log.warn("runId={} | source={} | entity={} | key={} | status=FAILED | reason={}",
            run.getId(), run.getSource(), item.type(), item.sourceKey(), exception.getMessage(),
            exception);
    }
}
