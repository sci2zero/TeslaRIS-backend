package rs.teslaris.migrator.dto;

import rs.teslaris.migrator.util.MigrationEntityType;

public record MigrationScheduleRequestDTO(
    String source,
    MigrationEntityType entityType,
    Integer batchSize,
    Boolean performIndex,
    Integer modifiedAfterDays,
    Boolean backfillOnlyCreated,
    Boolean isUpdate,
    Integer triggeredByUserId
) {

    public boolean shouldPerformIndex() {
        return Boolean.TRUE.equals(performIndex);
    }

    public boolean shouldUpdate() {
        return Boolean.TRUE.equals(isUpdate);
    }

    public boolean shouldBackfillOnlyCreated() {
        return Boolean.TRUE.equals(backfillOnlyCreated);
    }

    /**
     * The request of one firing, over the window the firing computes for itself.
     */
    public MigrationRequestDTO toRunRequest(String modifiedAfter) {
        return new MigrationRequestDTO(source, entityType, batchSize, performIndex, false,
            modifiedAfter, backfillOnlyCreated, isUpdate, triggeredByUserId);
    }
}
