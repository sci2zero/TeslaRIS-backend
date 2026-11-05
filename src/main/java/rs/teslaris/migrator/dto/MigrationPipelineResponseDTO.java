package rs.teslaris.migrator.dto;

import rs.teslaris.migrator.pipeline.MigrationPipeline;
import rs.teslaris.migrator.util.MigrationEntityType;

public record MigrationPipelineResponseDTO(
    String source,
    MigrationEntityType entityType,
    int defaultBatchSize,
    boolean running
) {

    public static MigrationPipelineResponseDTO of(MigrationPipeline<?> pipeline, boolean running) {
        return new MigrationPipelineResponseDTO(pipeline.sourceName(), pipeline.entityType(),
            pipeline.defaultBatchSize(), running);
    }
}
