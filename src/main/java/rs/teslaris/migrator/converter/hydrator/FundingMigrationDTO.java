package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.MigrationDTO;
import rs.teslaris.project.dto.funding.FundingDTO;

/**
 * Project, funder and currency are resolved at creation time.
 */
public record FundingMigrationDTO(
    String projectSourceKey,
    String sourceId,
    HydratorCVModel.Institution funder,
    FundingDTO funding,
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
