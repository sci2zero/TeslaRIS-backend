package rs.teslaris.revisioner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

public record DataQualityRemarkDTO(
    List<MultilingualContentDTO> title,

    List<MultilingualContentDTO> message,

    String target,

    double targetWeight,

    IssueSeverity severity,

    QualityDimension dimension,

    boolean blocking,

    double points,

    boolean usedForFairCompliance,

    Map<String, Object> constraints
) {
}
