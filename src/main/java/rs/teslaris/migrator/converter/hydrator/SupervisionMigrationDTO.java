package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.migrator.pipeline.MigrationDTO;

/**
 * The supervised thesis, created unless one with the same title and student already exists. The
 * curriculum owner is resolved at creation time.
 */
public record SupervisionMigrationDTO(
    String personSourceKey,
    String sourceKey,
    ThesisDTO thesis,
    String studentName,
    String rejection
) implements MigrationDTO {
}
