package rs.teslaris.migrator.converter.hydrator;

import java.util.List;
import rs.teslaris.core.dto.funding.FundingDTO;
import rs.teslaris.core.dto.project.PersonProjectContributionDTO;
import rs.teslaris.core.dto.project.ProjectDTO;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.MigrationDTO;

/**
 * A curriculum funding: the project it belongs to, the funding itself and the owner's part in it.
 * {@code reference} is the first strong one, {@code null} when there is none; the project is
 * looked up by it at creation time and created from {@code project} only when not found. Funder,
 * consortium, team and the owner are resolved at creation time.
 */
public record CvFundingMigrationDTO(
    String personSourceKey,
    String sourceId,
    HydratorFundingReference reference,
    ProjectDTO project,
    List<ProjectMigrationDTO.ConsortiumEntry> consortium,
    List<ProjectMigrationDTO.TeamEntry> investigators,
    PersonProjectContributionDTO owner,
    FundingDTO funding,
    HydratorCVModel.Institution funder,
    String rejection
) implements MigrationDTO {

    // Funding ids are unique only within a curriculum, hence the composite key
    public static String keyOf(String curriculumId, String fundingId) {
        return curriculumId + "#funding#" + fundingId;
    }

    @Override
    public String sourceKey() {
        return keyOf(personSourceKey, sourceId);
    }
}
