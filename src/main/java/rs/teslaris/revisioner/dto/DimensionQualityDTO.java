package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

public record DimensionQualityDTO(

    QualityDimension dimension,

    @Nullable
    Double averageScore,

    long openIssues,

    long affectedRecords
) {
}
