package rs.teslaris.core.assessment.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.ResultCalculationMethod;

public class ClassificationPriorityMapping {

    private static final Map<String, Integer> CLASSIFICATION_PRIORITIES = Map.ofEntries(
        Map.entry("M21APlus", 1),
        Map.entry("M21A", 2),
        Map.entry("M21", 3),
        Map.entry("M22", 4),
        Map.entry("M23", 5),
        Map.entry("M23e", 6),
        Map.entry("M24Plus", 7),
        Map.entry("M24", 8),
        Map.entry("M51", 9),
        Map.entry("M52", 10),
        Map.entry("M53", 11),
        Map.entry("M54", 12)
    );

    private static final Map<String, String> CLASSIFICATION_TO_ASSESSMENT_MAPPING = Map.ofEntries(
        Map.entry("M21APlus", "docM21APlus"),
        Map.entry("M21A", "docM21A"),
        Map.entry("M21", "docM21"),
        Map.entry("M22", "docM22"),
        Map.entry("M23", "docM23"),
        Map.entry("M23e", "docM23e"),
        Map.entry("M24Plus", "docM24Plus"),
        Map.entry("M24", "docM24"),
        Map.entry("M51", "docM51"),
        Map.entry("M52", "docM52"),
        Map.entry("M53", "docM53"),
        Map.entry("M54", "docM54")
    );

    public static Optional<AssessmentClassification> getClassificationBasedOnCriteria(
        ArrayList<AssessmentClassification> classifications,
        ResultCalculationMethod resultCalculationMethod) {
        return switch (resultCalculationMethod) {
            case BEST_VALUE -> classifications.stream()
                .min(Comparator.comparingInt(
                    assessmentClassification -> CLASSIFICATION_PRIORITIES.getOrDefault(
                        assessmentClassification.getCode(), Integer.MAX_VALUE)));
            case WORST_VALUE -> classifications.stream()
                .max(Comparator.comparingInt(
                    assessmentClassification -> CLASSIFICATION_PRIORITIES.getOrDefault(
                        assessmentClassification.getCode(), Integer.MIN_VALUE)));

            case null -> Optional.empty();
        };
    }

    public static Optional<String> getDocClassificationCodeBasedOnPubSeriesCode(
        String classificationCode) {
        var documentCode =
            CLASSIFICATION_TO_ASSESSMENT_MAPPING.getOrDefault(classificationCode, null);

        return Objects.nonNull(documentCode) ? Optional.of(documentCode) : Optional.empty();
    }
}
