package rs.teslaris.migrator.dto;

import java.time.Instant;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.MigrationRunStatus;
import rs.teslaris.migrator.util.MigrationEntityType;

public record MigrationRunResponseDTO(
    String runId,
    String source,
    MigrationEntityType entityType,
    MigrationRunStatus status,
    Instant startedAt,
    Instant finishedAt,
    int currentPage,
    long recordsRead,
    long itemsCreated,
    long itemsResolved,
    long itemsSkipped,
    long itemsFailed,
    long batchesFailed
) {

    public static MigrationRunResponseDTO of(MigrationRun run) {
        return new MigrationRunResponseDTO(
            run.getId(), run.getSource(), run.getEntityType(), run.getStatus(),
            run.getStartedAt(), run.getFinishedAt(), run.getCurrentPage(), run.getRecordsRead(),
            run.getItemsCreated(), run.getItemsResolved(), run.getItemsSkipped(),
            run.getItemsFailed(), run.getBatchesFailed());
    }
}
