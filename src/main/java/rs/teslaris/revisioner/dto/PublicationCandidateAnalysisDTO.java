package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;
import java.util.List;

public record PublicationCandidateAnalysisDTO(

    long publicationCandidates,

    long notPublicationCandidates,

    @Nullable
    Double candidateRate,

    long blockingConstraints,

    long blockingIssues,

    List<EntityTypeQualityDTO> candidateRateByEntityType,

    List<PrevalentIssueDTO> mostCommonBlockingConstraints
) {
}
