package rs.teslaris.migrator.converter.hydrator;

import java.util.List;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;

/**
 * Consortium OUs are resolved at creation time; a non-null {@code rejection} fails the item.
 */
public record ProjectMigrationDTO(
    String sourceId,
    ProjectDTO project,
    List<ConsortiumEntry> consortium,
    String rejection
) {

    public record ConsortiumEntry(
        OrganisationUnitProjectContributionType type,
        HydratorCVModel.Institution institution,
        int orderNumber
    ) {
    }
}
