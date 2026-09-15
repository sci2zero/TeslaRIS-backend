package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;
import java.util.List;

public record RepositoryOverviewDTO(

    @Nullable
    Double averageScore,

    @Nullable
    Double publicationCandidatePercentage,

    long openIssues,

    long recordsAssessed,

    List<EntityTypeQualityDTO> qualityByEntityType,

    List<PrevalentIssueDTO> issuesRequiringAttention
) {
}
