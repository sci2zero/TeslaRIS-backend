package rs.teslaris.migrator.dto;

import java.time.Instant;
import rs.teslaris.migrator.model.MigrationRecordLog;
import rs.teslaris.migrator.util.MigrationEntityType;

public record MigrationFailureDTO(
    MigrationEntityType entityType,
    String sourceKey,
    String reason,
    Instant processedAt
) {

    public static MigrationFailureDTO of(MigrationRecordLog entry) {
        return new MigrationFailureDTO(entry.getEntityType(), entry.getSourceKey(),
            entry.getReason(), entry.getProcessedAt());
    }
}
