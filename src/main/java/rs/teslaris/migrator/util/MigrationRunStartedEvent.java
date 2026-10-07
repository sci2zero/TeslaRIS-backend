package rs.teslaris.migrator.util;

/**
 * Published when a run starts, so converters can drop caches built from data that may have changed.
 */
public record MigrationRunStartedEvent(String runId, String source,
                                       MigrationEntityType entityType) {
}
