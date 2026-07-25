package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

public record DataQualityRuleResultDTO(

    String key,

    String target,

    QualityDimension dimension,

    IssueSeverity severity,

    boolean blocking,

    double points,

    boolean passed,

    List<MultilingualContentDTO> title,

    List<MultilingualContentDTO> message,

    @Nullable
    String actualValue
) {
}
