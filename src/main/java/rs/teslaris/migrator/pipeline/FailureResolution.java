package rs.teslaris.migrator.pipeline;

public record FailureResolution(
    FailureOutcome outcome,
    Integer resolvedEntityId
) {

    public static FailureResolution retry() {
        return new FailureResolution(FailureOutcome.RETRY, null);
    }

    public static FailureResolution skip() {
        return new FailureResolution(FailureOutcome.SKIP, null);
    }

    public static FailureResolution resolved(Integer resolvedEntityId) {
        return new FailureResolution(FailureOutcome.RESOLVED, resolvedEntityId);
    }
}
