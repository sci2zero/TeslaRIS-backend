package rs.teslaris.revisioner.dto;

import java.time.LocalDateTime;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

public record DataQualityIssueDTO(
    Integer assessmentId,

    String entityType,

    Integer entityId,

    String target,

    Integer recordMajorVersion,

    Integer recordMinorVersion,

    LocalDateTime assessmentDate,

    String ruleKey,

    QualityDimension dimension,

    IssueSeverity severity,

    boolean blocking,

    List<MultilingualContentDTO> title,

    List<MultilingualContentDTO> message,

    String entityNameSr,

    String entityNameOther
) {
}
