package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.core.dto.person.involvement.EmploymentDTO;

/**
 * An employment cannot be created from a DTO alone - it is added to a person, and the institution it
 * points at was migrated under a synthetic key. Both references are carried here and resolved
 * against the record log at creation time.
 */
public record EmploymentMigrationDTO(
    String personSourceKey,
    String institutionSourceKey,
    EmploymentDTO employment
) {
}
