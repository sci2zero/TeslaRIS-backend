package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;
import rs.teslaris.revisioner.util.dataquality.RepositoryEntityType;

public record EntityTypeQualityDTO(

    RepositoryEntityType entityType,

    long records,

    long affectedRecords,

    long openIssues,

    @Nullable
    Double averageScore,

    @Nullable
    Double publicationCandidatePercentage,

    boolean supported
) {

    public static EntityTypeQualityDTO unsupported(RepositoryEntityType entityType) {
        return new EntityTypeQualityDTO(entityType, 0, 0, 0, null, null, false);
    }
}
