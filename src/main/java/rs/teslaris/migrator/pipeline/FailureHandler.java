package rs.teslaris.migrator.pipeline;

/**
 * Decides what happens when a core create method throws.
 * <p>
 * This is where the recovery logic that lives inline in the OAI-PMH loader belongs: merge on
 * duplicate identifier, retry without identifiers, enrich an existing record. Transport failures are
 * not handled here - they are retried at batch level by the runner.
 */
@FunctionalInterface
public interface FailureHandler<D> {

    static <D> FailureHandler<D> noOp() {
        return (item, exception, attempt) -> FailureResolution.skip();
    }

    FailureResolution onCreateFailed(MigrationItem<D> item, Exception exception, int attempt);
}
