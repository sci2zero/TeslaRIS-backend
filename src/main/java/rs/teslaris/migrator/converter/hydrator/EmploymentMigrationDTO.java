package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.core.dto.person.involvement.EmploymentDTO;

/**
 * An employment cannot be created from a DTO alone - it is added to a person, and the institution it
 * points at was migrated under a synthetic key. Both references are carried here and resolved
 * against the record log at creation time. A non-null {@code rejection} means a source value was
 * invalid; the creator fails the item with it instead of creating the employment.
 */
public record EmploymentMigrationDTO(
    String personSourceKey,
    String institutionSourceKey,
    EmploymentDTO employment,
    String rejection
) {
}
