package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;
import rs.teslaris.revisioner.util.dataquality.RelatedEntityType;

public record RelatedQualityDTO(

    RelatedEntityType entityType,

    long linkedRecords,

    long affectedRecords,

    long openIssues,

    @Nullable
    Double averageScore,

    boolean supported
) {

    public static RelatedQualityDTO unsupported(RelatedEntityType entityType) {
        return new RelatedQualityDTO(entityType, 0, 0, 0, null, false);
    }
}
