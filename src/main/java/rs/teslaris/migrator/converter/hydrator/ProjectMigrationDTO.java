package rs.teslaris.migrator.converter.hydrator;

import java.util.List;
import rs.teslaris.core.dto.project.ProjectDTO;
import rs.teslaris.core.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectContributionType;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.model.hydrator.HydratorProjectModel;
import rs.teslaris.migrator.pipeline.MigrationDTO;

/**
 * Consortium OUs and team persons are resolved at creation time.
 */
public record ProjectMigrationDTO(
    String sourceId,
    ProjectDTO project,
    List<ConsortiumEntry> consortium,
    List<TeamEntry> team,
    String rejection
) implements MigrationDTO {

    // Consortium members have no id of their own; their position identifies them
    public static String consortiumKeyOf(String projectId, Integer orderNumber) {
        return projectId + "#consortium#" + orderNumber;
    }

    @Override
    public String sourceKey() {
        return sourceId;
    }

    public record TeamEntry(
        PersonProjectContributionType type,
        String name,
        String cienciaId,
        String orcid,
        int orderNumber
    ) {
    }

    // amount is set only when it becomes a funding part of this contribution
    public record ConsortiumEntry(
        OrganisationUnitProjectContributionType type,
        HydratorCVModel.Institution institution,
        int orderNumber,
        HydratorProjectModel.Amount amount
    ) {
    }
}
