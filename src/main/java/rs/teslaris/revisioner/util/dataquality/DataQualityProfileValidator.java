package rs.teslaris.revisioner.util.dataquality;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader.DataQualityProfile;

/**
 * Checks an uploaded policy before it reaches the configuration directory. The mapper is strict,
 * as the loader's mapper is, because a key the loader cannot read stops the application from
 * starting.
 */
public class DataQualityProfileValidator {

    private static final ObjectMapper strictObjectMapper = new ObjectMapper();

    private static final int MAX_REPORTED = 50;

    private static final List<String> EXPECTED_LANGUAGES = List.of("en", "sr");


    public static ValidationOutcome validate(String content) {
        Map<String, String> violations = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        DataQualityProfile profile;

        try {
            profile = strictObjectMapper.readValue(content, DataQualityProfile.class);
        } catch (UnrecognizedPropertyException e) {
            violations.put(path(e.getPropertyName()), "unknownPolicyPropertyMessage");
            return new ValidationOutcome(violations, warnings, null);
        } catch (MismatchedInputException e) {
            violations.put(path(e.getMessage()), "invalidPolicyValueMessage");
            return new ValidationOutcome(violations, warnings, null);
        } catch (JsonProcessingException e) {
            violations.put("file", "malformedPolicyJsonMessage");
            return new ValidationOutcome(violations, warnings, null);
        }

        validateProfileFields(profile, violations, warnings);
        validateTargetWeights(profile, violations);
        validateRemarks(profile, violations, warnings);

        return new ValidationOutcome(violations, warnings, violations.isEmpty() ? profile : null);
    }

    private static void validateProfileFields(DataQualityProfile profile,
                                              Map<String, String> violations,
                                              List<String> warnings) {
        var minimumRequiredScore = profile.minimumRequiredScore();

        if (Objects.isNull(minimumRequiredScore) || minimumRequiredScore < 0 ||
            minimumRequiredScore > 100) {
            violations.put("minimumRequiredScore", "policyScoreOutOfRangeMessage");
        }

        if (isBlankLocalisation(profile.name())) {
            warnings.add("name");
        } else {
            warnMissingLanguages(profile.name(), "name", warnings);
        }
    }

    private static void validateTargetWeights(DataQualityProfile profile,
                                              Map<String, String> violations) {
        if (Objects.isNull(profile.targetWeights())) {
            violations.put("targetWeights", "policyWithoutTargetWeightsMessage");
            return;
        }

        profile.targetWeights().forEach((target, weight) -> {
            if (Objects.isNull(target) || target.isBlank()) {
                report(violations, "targetWeights", "blankPolicyTargetMessage");
            } else if (Objects.isNull(weight) || weight <= 0) {
                report(violations, "targetWeights." + target, "policyWeightNotPositiveMessage");
            }
        });
    }

    private static void validateRemarks(DataQualityProfile profile,
                                        Map<String, String> violations,
                                        List<String> warnings) {
        var remarks = profile.dataQualityRemarks();

        if (Objects.isNull(remarks) || remarks.isEmpty()) {
            violations.put("dataQualityRemarks", "policyWithoutRulesMessage");
            return;
        }

        remarks.forEach((key, remark) -> {
            var field = "dataQualityRemarks." + key;

            if (Objects.isNull(key) || key.isBlank()) {
                report(violations, "dataQualityRemarks", "blankPolicyRuleKeyMessage");
                return;
            }

            // A target is a family and a field below it, and every filter splits on the dot.
            if (Objects.isNull(remark.target()) || !remark.target().contains(".")) {
                report(violations, field + ".target", "invalidPolicyRuleTargetMessage");
            }

            if (remark.points() < 0) {
                report(violations, field + ".points", "negativePolicyRulePointsMessage");
            }

            if (isBlankLocalisation(remark.title())) {
                report(violations, field + ".title", "blankPolicyLocalisationMessage");
            } else {
                warnMissingLanguages(remark.title(), field + ".title", warnings);
            }

            if (isBlankLocalisation(remark.message())) {
                report(violations, field + ".message", "blankPolicyLocalisationMessage");
            } else {
                warnMissingLanguages(remark.message(), field + ".message", warnings);
            }

            if (Objects.isNull(remark.metric())) {
                if (warnings.size() < MAX_REPORTED) {
                    warnings.add(field + ".metric");
                }
            } else if (!Objects.requireNonNullElse(profile.metricDefinitions(), Map.of())
                .containsKey(remark.metric())) {
                report(violations, field + ".metric", "unknownPolicyMetricMessage");
            }
        });
    }

    private static void warnMissingLanguages(Map<String, String> localisation, String field,
                                             List<String> warnings) {
        if (warnings.size() >= MAX_REPORTED) {
            return;
        }

        EXPECTED_LANGUAGES.stream()
            .filter(language -> Objects.isNull(localisation.get(language)) ||
                localisation.get(language).isBlank())
            .forEach(language -> warnings.add(field + "." + language));
    }

    private static boolean isBlankLocalisation(Map<String, String> localisation) {
        return Objects.isNull(localisation) || localisation.isEmpty() ||
            localisation.values().stream()
                .allMatch(value -> Objects.isNull(value) || value.isBlank());
    }

    private static void report(Map<String, String> violations, String field, String messageKey) {
        if (violations.size() < MAX_REPORTED) {
            violations.putIfAbsent(field, messageKey);
        }
    }

    private static String path(String pathReference) {
        return Objects.isNull(pathReference) || pathReference.isBlank() ? "file" : pathReference;
    }

    public record ValidationOutcome(

        Map<String, String> violations,

        List<String> warnings,

        DataQualityProfile profile
    ) {
        public boolean valid() {
            return violations.isEmpty();
        }
    }
}
