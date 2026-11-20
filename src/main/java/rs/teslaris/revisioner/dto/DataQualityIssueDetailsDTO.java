package rs.teslaris.revisioner.dto;

import java.time.Instant;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

public record DataQualityIssueDetailsDTO(

    Integer assessmentId,

    String ruleKey,

    String entityType,

    Integer entityId,

    String entityNameSr,

    String entityNameOther,

    Integer recordMajorVersion,

    Integer recordMinorVersion,

    Instant assessmentDate,

    Double score,

    Double maximumScore,

    List<DataQualityIssueOccurrenceDTO> occurrences,

    List<MultilingualContentDTO> title,

    IssueSeverity severity,

    String targetEntityType,

    String targetObject,

    Double constraintWeight,

    boolean fairRelated,

    boolean blocking,

    String policy,

    String policyVersion,

    List<MultilingualContentDTO> policyName,

    QualityDimension dimension,

    List<MultilingualContentDTO> dimensionDefinition,

    String metric,

    List<MultilingualContentDTO> metricTitle,

    List<MultilingualContentDTO> metricDefinition
) {
}
