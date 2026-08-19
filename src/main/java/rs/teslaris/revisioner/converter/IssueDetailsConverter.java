package rs.teslaris.revisioner.converter;

import java.util.List;
import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.revisioner.dto.DataQualityIssueDetailsDTO;
import rs.teslaris.revisioner.dto.DataQualityIssueOccurrenceDTO;
import rs.teslaris.revisioner.model.qualityassessment.ConstraintEvaluationResult;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;

public class IssueDetailsConverter {

    public static DataQualityIssueDetailsDTO toDTO(DataQualityAssessment assessment,
                                                   String ruleKey,
                                                   List<ConstraintEvaluationResult> occurrences) {
        var profileName = assessment.getProfileName();
        var profileVersion = assessment.getProfileVersion();
        var revision = assessment.getRevision();

        var remark = DataQualityAssessmentConfigurationLoader.getIssue(
            profileName, profileVersion, ruleKey);

        var target = remark.target();

        return new DataQualityIssueDetailsDTO(
            assessment.getId(),
            ruleKey,
            revision.getEntityType(),
            revision.getEntityId(),
            revision.getMajorVersion(),
            revision.getMinorVersion(),
            assessment.getFinishedAt(),
            0.0,
            DataQualityAssessmentConfigurationLoader.getWeightedPoints(profileName, profileVersion,
                remark),
            occurrences.stream()
                .map(occurrence -> toOccurrenceDTO(profileName, profileVersion, ruleKey,
                    occurrence))
                .toList(),
            MultilingualContentConverter.getMultilingualContentDTO(
                DataQualityAssessmentConfigurationLoader.getDataQualityTitle(profileName,
                    profileVersion, ruleKey)),
            remark.severity(),
            targetEntityType(target),
            target,
            DataQualityAssessmentConfigurationLoader.getTargetWeights(profileName, profileVersion)
                .getOrDefault(target, 1.0),
            remark.usedForFairCompliance(),
            remark.blocking(),
            profileName,
            profileVersion,
            remark.dimension(),
            MultilingualContentConverter.getMultilingualContentDTO(
                DataQualityAssessmentConfigurationLoader.getDimensionDefinition(profileName,
                    profileVersion, remark.dimension()))
        );
    }

    private static DataQualityIssueOccurrenceDTO toOccurrenceDTO(String profileName,
                                                                 String profileVersion,
                                                                 String ruleKey,
                                                                 ConstraintEvaluationResult occurrence) {
        var parameters = Objects.requireNonNullElse(occurrence.getParameters(), List.<String>of());

        return new DataQualityIssueOccurrenceDTO(
            parameters,
            MultilingualContentConverter.getMultilingualContentDTO(
                DataQualityAssessmentConfigurationLoader.getDataQualityRemark(profileName,
                    profileVersion, ruleKey, parameters.toArray()))
        );
    }

    private static String targetEntityType(String target) {
        if (Objects.isNull(target)) {
            return null;
        }

        var separator = target.indexOf('.');

        return separator > 0 ? target.substring(0, separator) : target;
    }
}
