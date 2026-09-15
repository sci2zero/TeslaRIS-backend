package rs.teslaris.revisioner.converter;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.revisioner.dto.DataQualityAssessmentDTO;
import rs.teslaris.revisioner.dto.DataQualityAssessmentSimpleDTO;
import rs.teslaris.revisioner.dto.DataQualityRuleResultDTO;
import rs.teslaris.revisioner.model.qualityassessment.ConstraintEvaluationResult;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentListener;

public class DataQualityAssessmentConverter {

    /**
     * The first parameter of every rule is the value that made it fail; the rest describe the
     * constraint it was measured against, so only the first one is worth showing as the actual
     * value.
     */
    @Nullable
    private static String describeOccurrences(List<ConstraintEvaluationResult> occurrences) {
        var values = occurrences.stream()
            .map(ConstraintEvaluationResult::getParameters)
            .filter(parameters -> !parameters.isEmpty())
            .map(List::getFirst)
            .filter(value -> Objects.nonNull(value) && !value.isBlank())
            .toList();

        return values.isEmpty() ? null : String.join("; ", values);
    }

    /**
     * Every occurrence resolves its own message from its own parameters, and the messages of one
     * rule are merged per language into the single message its row shows.
     */
    private static List<MultilingualContentDTO> mergeMessages(DataQualityAssessment assessment,
                                                              String key,
                                                              List<ConstraintEvaluationResult> occurrences) {
        var merged = new LinkedHashMap<String, MultilingualContentDTO>();

        occurrences.forEach(occurrence ->
            MultilingualContentConverter.getMultilingualContentDTO(
                    DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                        assessment.getProfileName(),
                        assessment.getProfileVersion(),
                        key,
                        occurrence.getParameters().toArray()
                    ))
                .forEach(message -> merged.merge(
                    message.getLanguageTag(),
                    new MultilingualContentDTO(message),
                    (existing, addition) -> {
                        existing.setContent(existing.getContent() + "\n\n" + addition.getContent());
                        return existing;
                    }))
        );

        return List.copyOf(merged.values());
    }

    public static DataQualityAssessmentDTO toDTO(DataQualityAssessment assessment) {
        var targets = DataQualityAssessmentListener.resolveTargetTypes(
            assessment.getRevision().getEntityType());

        // A rule checked per title or per contributor fails once per offending value, so the same
        // key can be recorded several times - the table shows one row per rule and lists every
        // value behind it.
        Map<String, List<ConstraintEvaluationResult>> failedIssues =
            assessment.getIssues().stream()
                .collect(Collectors.groupingBy(ConstraintEvaluationResult::getKey));

        var passedRules = new ArrayList<DataQualityRuleResultDTO>();
        var failedRules = new ArrayList<DataQualityRuleResultDTO>();

        for (var entry : DataQualityAssessmentConfigurationLoader.getRulesForTarget(
            assessment.getProfileName(),
            assessment.getProfileVersion(),
            targets)) {

            String key = entry.getKey();
            var remark = entry.getValue();

            var occurrences = failedIssues.get(key);

            if (Objects.isNull(occurrences) || occurrences.isEmpty()) {
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
                        mergeMessages(assessment, key, occurrences),
                        describeOccurrences(occurrences)
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
