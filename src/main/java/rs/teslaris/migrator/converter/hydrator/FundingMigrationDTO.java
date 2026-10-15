package rs.teslaris.migrator.converter.hydrator;

import java.util.List;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.MigrationDTO;
import rs.teslaris.project.dto.funding.FundingDTO;

/**
 * Project, funder and currency are resolved at creation time. {@code consortiumAmounts} become
 * parts of this funding when it is the project's only one.
 */
public record FundingMigrationDTO(
    String projectSourceKey,
    String sourceId,
    HydratorCVModel.Institution funder,
    FundingDTO funding,
    List<ProjectMigrationDTO.ConsortiumEntry> consortiumAmounts,
    String rejection
) implements MigrationDTO {

    // Funding ids are unique only within a project, hence the composite key
    public static String keyOf(String projectId, String fundingId) {
        return projectId + "#funding#" + fundingId;
    }

    @Override
    public String sourceKey() {
        return keyOf(projectSourceKey, sourceId);
    }
}
