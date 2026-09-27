package rs.teslaris.revisioner.dto;

import java.util.List;
import java.util.Map;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

// The message template is left out - the table never shows it and it is most of a rule's payload.
public record PolicyConstraintDTO(

    String key,

    List<MultilingualContentDTO> title,

    String target,

    double targetWeight,

    QualityDimension dimension,

    IssueSeverity severity,

    boolean blocking,

    double points,

    boolean usedForFairCompliance,

    Map<String, Object> constraints,

    long affectedRecords
) {
}
