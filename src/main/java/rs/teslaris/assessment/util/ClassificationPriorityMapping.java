package rs.teslaris.assessment.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.core.model.document.PublicationType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.ResultCalculationMethod;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.files.ConfigurationLoaderUtil;
import rs.teslaris.core.util.functional.Pair;

@Component
public class ClassificationPriorityMapping {

    private static ProceedingsPublicationRepository proceedingsPublicationRepository;

    private static JournalPublicationRepository journalPublicationRepository;

    private static String externalOverrideConfiguration;

    private static AssessmentConfig assessmentConfig;


    @Autowired
    public ClassificationPriorityMapping(
        ProceedingsPublicationRepository proceedingsPublicationRepository,
        JournalPublicationRepository journalPublicationRepository,
        @Value("${assessment.classifications.priority-mapping}")
        String externalOverrideConfiguration) {
        ClassificationPriorityMapping.proceedingsPublicationRepository =
            proceedingsPublicationRepository;
        ClassificationPriorityMapping.journalPublicationRepository = journalPublicationRepository;
        ClassificationPriorityMapping.externalOverrideConfiguration = externalOverrideConfiguration;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    protected static void reloadConfiguration() {
        try {
            assessmentConfig = ConfigurationLoaderUtil.loadConfiguration(AssessmentConfig.class,
                "src/main/resources/assessment/assessmentConfiguration.json",
                externalOverrideConfiguration);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload indicator mapping configuration: " + e.getMessage());
        }
    }

    public static Optional<Pair<AssessmentClassification, Set<MultiLingualContent>>> getClassificationBasedOnCriteria(
        List<Pair<AssessmentClassification, Set<MultiLingualContent>>> classifications,
        ResultCalculationMethod resultCalculationMethod) {
        return switch (resultCalculationMethod) {
            case BEST_VALUE -> classifications.stream()
                .min(Comparator.comparingInt(
                    assessmentClassification -> assessmentConfig.classificationPriorities.getOrDefault(
                        assessmentClassification.a.getCode(), Integer.MAX_VALUE)));
            case WORST_VALUE -> classifications.stream()
                .max(Comparator.comparingInt(
                    assessmentClassification -> assessmentConfig.classificationPriorities.getOrDefault(
                        assessmentClassification.a.getCode(), Integer.MIN_VALUE)));
        };
    }

    public static Optional<AssessmentClassification> getBestJournalClassification(
        List<AssessmentClassification> classificationCodes) {
        return classificationCodes.stream().min(Comparator.comparingInt(
            assessmentClassification -> assessmentConfig.classificationPriorities.getOrDefault(
                assessmentClassification.getCode(), Integer.MAX_VALUE)));
    }

    public static Optional<String> getDocClassificationCodeBasedOnCode(
        String classificationCode, Integer documentId) {

        String documentCode =
            assessmentConfig.classificationToAssessmentMapping.getOrDefault(classificationCode,
                null);

        if (Objects.isNull(documentCode)) {
            return Optional.empty();
        }

        if (documentCode.equals("M30") || documentCode.equals("M60")) {
            return proceedingsPublicationRepository.findById(documentId)
                .flatMap(proceedingsPublication -> getMappedCode(documentCode,
                    proceedingsPublication.getProceedingsPublicationType()));
        }

        if (documentCode.equals("M24") ||
            assessmentConfig.classificationPriorities.get(classificationCode) <
                assessmentConfig.classificationPriorities.get("journalM24")) {
            var journalPublicationOptional = journalPublicationRepository.findById(documentId);

            if (journalPublicationOptional.isEmpty()) {
                var proceedingsPublicationOptional =
                    proceedingsPublicationRepository.findById(documentId);

                return proceedingsPublicationOptional.isPresent() ? Optional.of(documentCode) :
                    Optional.empty();
            }

            return journalPublicationOptional.flatMap(
                journalPublication -> getMappedCode(documentCode,
                    journalPublication.getJournalPublicationType()));
        }

        return Optional.of(documentCode);
    }

    @Nullable
    public static String getImaginaryDocClassificationCodeBasedOnCode(
        String classificationCode, PublicationType publicationType) {

        var documentCode =
            assessmentConfig.classificationToAssessmentMapping.get(classificationCode);

        if (Objects.isNull(documentCode)) {
            return null;
        }

        if (publicationType instanceof JournalPublicationType) {
            if (documentCode.equals("M24") ||
                assessmentConfig.classificationPriorities.get(classificationCode) <
                    assessmentConfig.classificationPriorities.get("journalM24")) {
                return getMappedCode(documentCode, (JournalPublicationType) publicationType).orElse(
                    null);
            }
        } else if (publicationType instanceof ProceedingsPublicationType) {
            if (documentCode.equals("M30") || documentCode.equals("M60")) {
                return getMappedCode(documentCode,
                    (ProceedingsPublicationType) publicationType).orElse(null);
            }
        }

        return documentCode;
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
            baseCode.equals("M30") ? mappingM30.getOrDefault(type, null) :
                mappingM60.getOrDefault(type, null)
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
            baseCode.equals("M24") ? mappingM26.getOrDefault(type, baseCode) :
                mappingM27.getOrDefault(type, baseCode)
        );
    }

    public static List<String> getAssessmentGroups() {
        return assessmentConfig.groupToClassificationsMapping.keySet().stream().sorted().toList();
    }

    public static boolean existsInGroup(String groupCode, String classificationCode) {
        return assessmentConfig.groupToClassificationsMapping.getOrDefault(groupCode,
                Collections.emptyList())
            .contains(classificationCode);
    }

    public static String getCodeDisplayValue(String code) {
        return code.toLowerCase()
            .replace("m", "M")
            .replace("plus", "+");
    }

    public static String getCodeOriginalValue(String displayValue) {
        return displayValue.toUpperCase()
            .replace("+", "Plus")
            .replace("E", "e");
    }

    public static boolean isOnSciList(String assessmentCode) {
        return assessmentConfig.sciList().contains(getCodeOriginalValue(assessmentCode));
    }

    public static int getSciListPriority(String assessmentCode) {
        return assessmentConfig.sciListPriorities().get(getCodeOriginalValue(assessmentCode));
    }

    @Nullable
    public static String getGroupCode(String assessmentCode) {
        AtomicReference<String> resultingGroupCode = new AtomicReference<>();
        assessmentConfig.groupToClassificationsMapping.forEach(
            (groupCode, relatedAssessmentCodes) -> {
                if (relatedAssessmentCodes.contains(getCodeOriginalValue(assessmentCode))) {
                    resultingGroupCode.set(groupCode);
                }
            });

        return resultingGroupCode.get();
    }

    public static String getGroupNameBasedOnCode(String groupCode, String locale) {
        return assessmentConfig.groupToNameMapping.getOrDefault(groupCode, new HashMap<>())
            .getOrDefault(locale, "");
    }

    public static boolean canPublicationTypeBeClassifiedAsCode(String publicationType,
                                                               String documentClassificationCode) {
        var mapping = assessmentConfig.typeToSupportedClassifications.getOrDefault(publicationType,
            new ArrayList<>());

        return mapping.contains(documentClassificationCode) || mapping.contains("ALL");
    }

    public static boolean meetsPageRequirements(DocumentPublicationIndex document) {
        var minimumPageCount = assessmentConfig.minimumPageRequirements
            .getOrDefault(document.getPublicationType(), 0);

        if (minimumPageCount == 0) {
            return true;
        }

        if (Objects.isNull(document.getNumberOfPages())) {
            return false;
        }

        return document.getNumberOfPages() >= minimumPageCount;
    }

    @Nullable
    public static String getAssessmentCodeForThesisType(ThesisType thesisType) {
        return assessmentConfig.defendedThesesMapping().getOrDefault(thesisType, null);
    }

    private record AssessmentConfig(
        @JsonProperty("classificationPriorities") Map<String, Integer> classificationPriorities,
        @JsonProperty("classificationToAssessmentMapping") Map<String, String> classificationToAssessmentMapping,
        @JsonProperty("groupToClassificationsMapping") Map<String, List<String>> groupToClassificationsMapping,
        @JsonProperty("groupToNameMapping") Map<String, Map<String, String>> groupToNameMapping,
        @JsonProperty("sciList") List<String> sciList,
        @JsonProperty("sciListPriorities") Map<String, Integer> sciListPriorities,
        @JsonProperty("typeToSupportedClassifications") Map<String, List<String>> typeToSupportedClassifications,
        @JsonProperty("minimumPageRequirements") Map<String, Integer> minimumPageRequirements,
        @JsonProperty("defendedThesesMapping") Map<ThesisType, String> defendedThesesMapping
    ) {
    }
}
