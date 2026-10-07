package rs.teslaris.migrator.converter.hydrator;

import java.util.List;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.project.model.project.PersonProjectContributionType;

/**
 * Consortium OUs and team persons are resolved at creation time; a non-null {@code rejection} fails the item.
 */
public record ProjectMigrationDTO(
    String sourceId,
    ProjectDTO project,
    List<ConsortiumEntry> consortium,
    List<TeamEntry> team,
    String rejection
) {

    public record TeamEntry(
        PersonProjectContributionType type,
        String name,
        String cienciaId,
        String orcid,
        int orderNumber
    ) {
    }

    public record ConsortiumEntry(
        OrganisationUnitProjectContributionType type,
        HydratorCVModel.Institution institution,
        int orderNumber
    ) {
    }
}
