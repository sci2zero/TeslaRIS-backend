package rs.teslaris.revisioner.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import rs.teslaris.revisioner.model.qualityassessment.DimensionScore;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

public record DataQualityAssessmentDTO(

    Integer assessmentId,

    String profileName,

    String profileVersion,

    String engineVersion,

    Instant startedAt,

    Instant finishedAt,

    boolean valid,

    double qualityScore,

    double qualityScoreFair,

    double totalPoints,

    double totalPointsFair,

    double achievedPoints,

    double achievedFairPoints,

    int passedRules,

    int warningFailedRules,

    int errorFailedRules,

    Map<QualityDimension, DimensionScore> dimensionScores,

    List<DataQualityRuleResultDTO> passedRulesList,

    List<DataQualityRuleResultDTO> failedRulesList
) {
}
