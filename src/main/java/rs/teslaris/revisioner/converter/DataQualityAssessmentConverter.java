package rs.teslaris.revisioner.converter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.revisioner.dto.DataQualityAssessmentDTO;
import rs.teslaris.revisioner.dto.DataQualityAssessmentSimpleDTO;
import rs.teslaris.revisioner.dto.DataQualityRuleResultDTO;
import rs.teslaris.revisioner.model.qualityassessment.ConstraintEvaluationResult;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentListener;

public class DataQualityAssessmentConverter {

    public static DataQualityAssessmentDTO toDTO(DataQualityAssessment assessment) {
        var targets = DataQualityAssessmentListener.resolveTargetTypes(
            assessment.getRevision().getEntityType());

        Map<String, ConstraintEvaluationResult> failedIssues =
            assessment.getIssues().stream()
                .collect(Collectors.toMap(
                    ConstraintEvaluationResult::getKey,
                    Function.identity()));

        var passedRules = new ArrayList<DataQualityRuleResultDTO>();
        var failedRules = new ArrayList<DataQualityRuleResultDTO>();

        for (var entry : DataQualityAssessmentConfigurationLoader.getRulesForTarget(
            assessment.getProfileName(),
            assessment.getProfileVersion(),
            targets)) {

            String key = entry.getKey();
            var remark = entry.getValue();

            ConstraintEvaluationResult issue = failedIssues.get(key);

            if (Objects.isNull(issue)) {
                passedRules.add(
                    new DataQualityRuleResultDTO(
                        key,
                        remark.target(),
                        remark.dimension(),
                        remark.severity(),
                        remark.blocking(),
                        remark.points(),
                        true,
                        MultilingualContentConverter.getMultilingualContentDTO(
                            DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                                assessment.getProfileName(),
                                assessment.getProfileVersion(),
                                key
                            )
                        ),
                        new ArrayList<>(),
                        null
                    )
                );
            } else {
                failedRules.add(
                    new DataQualityRuleResultDTO(
                        key,
                        remark.target(),
                        remark.dimension(),
                        remark.severity(),
                        remark.blocking(),
                        remark.points(),
                        false,
                        MultilingualContentConverter.getMultilingualContentDTO(
                            DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                                assessment.getProfileName(),
                                assessment.getProfileVersion(),
                                key
                            )
                        ),
                        MultilingualContentConverter.getMultilingualContentDTO(
                            DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                                assessment.getProfileName(),
                                assessment.getProfileVersion(),
                                key,
                                issue.getParameters().toArray()
                            )
                        ),
                        issue.getParameters().isEmpty()
                            ? null
                            : String.join(", ", issue.getParameters())
                    )
                );
            }
        }

        passedRules.sort(Comparator
            .comparing(DataQualityRuleResultDTO::dimension)
            .thenComparing(DataQualityRuleResultDTO::severity)
            .thenComparing(DataQualityRuleResultDTO::key)
        );

        failedRules.sort(Comparator
            .comparing(DataQualityRuleResultDTO::dimension)
            .thenComparing(DataQualityRuleResultDTO::severity)
            .thenComparing(DataQualityRuleResultDTO::key)
        );

        return new DataQualityAssessmentDTO(
            assessment.getId(),
            assessment.getProfileName(),
            assessment.getProfileVersion(),
            assessment.getEngineVersion(),
            assessment.getStartedAt(),
            assessment.getFinishedAt(),
            assessment.getValid(),
            assessment.getQualityScore(),
            assessment.getQualityScoreFair(),
            assessment.getTotalPoints(),
            assessment.getTotalPointsFair(),
            assessment.getAchievedPointsNormalised(),
            assessment.getAchievedFairPointsNormalised(),
            assessment.getPassedRules(),
            assessment.getWarningFailedRules(),
            assessment.getErrorFailedRules(),
            assessment.getDimensionScores(),
            passedRules,
            failedRules
        );
    }

    public static DataQualityAssessmentSimpleDTO toSimpleDTO(DataQualityAssessment assessment) {
        return new DataQualityAssessmentSimpleDTO(
            assessment.getProfileName(),
            assessment.getProfileVersion(),
            assessment.getQualityScore(),
            Boolean.TRUE.equals(assessment.getPublicationCandidate()),
            LocalDate.ofInstant(assessment.getFinishedAt(), ZoneId.systemDefault())
        );
    }
}
