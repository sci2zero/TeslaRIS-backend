package rs.teslaris.core.assessment.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.ResultCalculationMethod;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;

@Component
public class ClassificationPriorityMapping {

    private static final Map<String, Integer> CLASSIFICATION_PRIORITIES = Map.ofEntries(
        // JOURNALS
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
        Map.entry("M54", 12),

        // CONFERENCES
        Map.entry("multinationalConf", 13),
        Map.entry("nationalConf", 14)
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
        Map.entry("M54", "docM54"),
        Map.entry("multinationalConf", "M30"),
        Map.entry("nationalConf", "M60")
    );

    private static final Map<String, List<String>> GROUP_TO_CLASSIFICATIONS_MAPPING = Map.ofEntries(
        Map.entry("M10", List.of("M11", "M12", "M13", "M14", "M15", "M16")),
        Map.entry("M20",
            List.of("docM21APlus", "docM21A", "docM21", "docM22", "docM23", "docM23e", "docM24Plus",
                "docM24")),
        Map.entry("M30", List.of("M31", "M32", "M33", "M34")),
        Map.entry("M40", List.of("M41", "M42", "M43", "M44", "M45", "M46", "M47")),
        Map.entry("M50", List.of("docM51", "docM52", "docM53", "docM54", "M56", "M57")),
        Map.entry("M60", List.of("M61", "M62", "M63", "M64", "M67", "M68", "M69")),
        Map.entry("M70", List.of("M70")),
        Map.entry("M80", List.of("M81", "M82", "M83", "M84")),
        Map.entry("M90", List.of("M91a", "M91", "M62", "M93", "M94", "M95", "M96", "M97", "M98"))
    );

    private static ProceedingsPublicationRepository proceedingsPublicationRepository;

    private static JournalPublicationRepository journalPublicationRepository;

    @Autowired
    public ClassificationPriorityMapping(
        ProceedingsPublicationRepository proceedingsPublicationRepository,
        JournalPublicationRepository journalPublicationRepository) {
        ClassificationPriorityMapping.proceedingsPublicationRepository =
            proceedingsPublicationRepository;
        ClassificationPriorityMapping.journalPublicationRepository = journalPublicationRepository;
    }


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
        };
    }

    public static Optional<String> getDocClassificationCodeBasedOnCode(
        String classificationCode, Integer documentId) {

        String documentCode = CLASSIFICATION_TO_ASSESSMENT_MAPPING.get(classificationCode);

        if (Objects.isNull(documentCode)) {
            return Optional.empty();
        }

        if (documentCode.equals("M30") || documentCode.equals("M60")) {
            return proceedingsPublicationRepository.findById(documentId)
                .flatMap(proceedingsPublication -> getMappedCode(documentCode,
                    proceedingsPublication.getProceedingsPublicationType()));
        }

        if (documentCode.equals("docM24") || CLASSIFICATION_PRIORITIES.get(classificationCode) <
            CLASSIFICATION_PRIORITIES.get("M24")) {
            return journalPublicationRepository.findById(documentId).flatMap(
                journalPublication -> getMappedCode(documentCode,
                    journalPublication.getJournalPublicationType()));
        }

        return Optional.of(documentCode);
    }

    private static Optional<String> getMappedCode(String baseCode,
                                                  ProceedingsPublicationType type) {
        Map<ProceedingsPublicationType, String> mappingM30 = Map.of(
            ProceedingsPublicationType.INVITED_FULL_ARTICLE, "M31",
            ProceedingsPublicationType.INVITED_ABSTRACT_ARTICLE, "M32",
            ProceedingsPublicationType.REGULAR_FULL_ARTICLE, "M33",
            ProceedingsPublicationType.REGULAR_ABSTRACT_ARTICLE, "M34"
        );

        Map<ProceedingsPublicationType, String> mappingM60 = Map.of(
            ProceedingsPublicationType.INVITED_FULL_ARTICLE, "M61",
            ProceedingsPublicationType.INVITED_ABSTRACT_ARTICLE, "M62",
            ProceedingsPublicationType.REGULAR_FULL_ARTICLE, "M63",
            ProceedingsPublicationType.REGULAR_ABSTRACT_ARTICLE, "M64",
            ProceedingsPublicationType.SCIENTIFIC_CRITIC, "M69"
        );

        return Optional.ofNullable(
            baseCode.equals("M30") ? mappingM30.get(type) : mappingM60.get(type)
        );
    }

    private static Optional<String> getMappedCode(String baseCode,
                                                  JournalPublicationType type) {
        Map<JournalPublicationType, String> mappingM26 = Map.of(
            JournalPublicationType.SCIENTIFIC_CRITIC, "M26"
        );

        Map<JournalPublicationType, String> mappingM27 = Map.of(
            JournalPublicationType.SCIENTIFIC_CRITIC, "M27"
        );

        return Optional.ofNullable(
            baseCode.equals("docM24") ? mappingM26.getOrDefault(type, baseCode) :
                mappingM27.getOrDefault(type, baseCode)
        );
    }

    public static List<String> getAssessmentGroups() {
        return GROUP_TO_CLASSIFICATIONS_MAPPING.keySet().stream().sorted().toList();
    }

    public static boolean existsInGroup(String groupCode, String classificationCode) {
        return GROUP_TO_CLASSIFICATIONS_MAPPING.getOrDefault(groupCode, List.of())
            .contains(classificationCode);
    }
}
