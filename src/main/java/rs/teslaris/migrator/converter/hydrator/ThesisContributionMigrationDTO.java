package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.migrator.pipeline.MigrationDTO;

/**
 * A thesis the curriculum owner contributed to as advisor or examiner, created unless one with
 * the same title and student already exists. The owner is resolved at creation time.
 */
public record ThesisContributionMigrationDTO(
    String personSourceKey,
    String sourceKey,
    ThesisDTO thesis,
    String studentName,
    String rejection
) implements MigrationDTO {
}
