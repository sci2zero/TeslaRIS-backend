package rs.teslaris.migrator.dto;

import rs.teslaris.migrator.util.MigrationEntityType;

public record MigrationRequestDTO(
    String source,
    MigrationEntityType entityType,
    Integer batchSize,
    Boolean performIndex,
    Boolean resume,
    String modifiedAfter,
    Integer triggeredByUserId
) {

    public boolean shouldResume() {
        return Boolean.TRUE.equals(resume);
    }

    public boolean shouldPerformIndex() {
        return Boolean.TRUE.equals(performIndex);
    }
}
