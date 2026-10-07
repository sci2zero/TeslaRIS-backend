package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.project.dto.project.ProjectDTO;

/**
 * A non-null {@code rejection} fails the item.
 */
public record ProjectMigrationDTO(
    String sourceId,
    ProjectDTO project,
    String rejection
) {
}
