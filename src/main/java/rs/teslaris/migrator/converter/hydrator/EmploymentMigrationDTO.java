package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;

/**
 * Person and organisation unit are resolved at creation time; a non-null {@code rejection} fails
 * the item.
 */
public record EmploymentMigrationDTO(
    String personSourceKey,
    String sourceId,
    HydratorCVModel.Institution institution,
    EmploymentDTO employment,
    String rejection
) {
}
