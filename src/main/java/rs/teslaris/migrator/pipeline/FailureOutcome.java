package rs.teslaris.migrator.pipeline;

public enum FailureOutcome {

    /**
     * Try the creation again, if the retry policy allows it.
     */
    RETRY,

    /**
     * The situation was handled without creating a new entity (merged into an existing one, an
     * identifier was enriched, ...). Not an error, not retried.
     */
    RESOLVED,

    /**
     * Nothing can be done - record the failure and move on.
     */
    SKIP
}
