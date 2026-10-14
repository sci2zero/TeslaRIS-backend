package rs.teslaris.migrator.dto;

import rs.teslaris.migrator.util.MigrationEntityType;

public record MigrationRequestDTO(
    String source,
    MigrationEntityType entityType,
    Integer batchSize,
    Boolean performIndex,
    Boolean resume,
    String modifiedAfter,
    Boolean backfillOnlyCreated,
    Integer triggeredByUserId
) {

    public boolean shouldResume() {
        return Boolean.TRUE.equals(resume);
    }

    public boolean shouldPerformIndex() {
        return Boolean.TRUE.equals(performIndex);
    }

    /**
     * Whether the follow-up backfill should be limited to the entities this run created, rather
     * than sweeping every entity of the types it touched. Worth setting for small runs, where the
     * precise set is cheaper than a repository-wide sweep.
     */
    public boolean shouldBackfillOnlyCreated() {
        return Boolean.TRUE.equals(backfillOnlyCreated);
    }
}
