package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.migrator.pipeline.MigrationDTO;

/**
 * A funding-output pair of a curriculum: the record log keys of both sides, resolved to the
 * project and the document when the item is created, so outputs of the same run are found.
 */
public record ProjectDocumentMigrationDTO(
    String sourceKey,
    String fundingKey,
    String outputKey,
    String rejection
) implements MigrationDTO {
}
