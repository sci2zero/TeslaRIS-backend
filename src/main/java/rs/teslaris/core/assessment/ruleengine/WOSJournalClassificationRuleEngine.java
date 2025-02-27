package rs.teslaris.core.assessment.ruleengine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.EntityIndicatorSource;
import rs.teslaris.core.assessment.model.PublicationSeriesIndicator;
import rs.teslaris.core.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.assessment.util.AssessmentRulesConfigurationLoader;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.repository.document.JournalRepository;

@Transactional
public class WOSJournalClassificationRuleEngine extends JournalClassificationRuleEngine {

    @Override
    public void initialize(
        PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository,
        JournalRepository journalRepository, JournalIndexRepository journalIndexRepository,
        PublicationSeriesAssessmentClassificationRepository assessmentClassificationRepository,
        AssessmentClassificationService assessmentClassificationService) {
        this.publicationSeriesIndicatorRepository = publicationSeriesIndicatorRepository;
        this.journalRepository = journalRepository;
        this.journalIndexRepository = journalIndexRepository;
        this.assessmentClassificationRepository = assessmentClassificationRepository;
        this.assessmentClassificationService = assessmentClassificationService;
        this.source = EntityIndicatorSource.WEB_OF_SCIENCE;
    }

    @Nullable
    @Override
    @Transactional
    public AssessmentClassification handleM21APlus(String category) {
        return handlePercentileClassification("journalM21APlus", category, 0.05, "M21APlus");
    }

    @Nullable
    @Override
    public AssessmentClassification handleM21A(String category) {
        return handlePercentileClassification("journalM21A", category, 0.15, "M21A");
    }

    @Nullable
    @Override
    public AssessmentClassification handleM21(String category) {
        return handlePercentileClassification("journalM21", category, 0.35, "M21");
    }

    @Nullable
    @Override
    public AssessmentClassification handleM22(String category) {
        return handlePercentileClassification("journalM22", category, 0.75, "M22");
    }

    @Nullable
    @Override
    public AssessmentClassification handleM23(String category) {
        return handlePercentileClassification("journalM23", category, 1.0, "M23");
    }

    @Nullable
    @Override
    public AssessmentClassification handleM23e(String category) {
        return null;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM24plus(String category) {
        var jcr = findIndicatorByCode("jcr", null);
        var jcrConditionPassed = Objects.nonNull(jcr) && jcr.getBooleanValue();

        if (jcrConditionPassed) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("journalClassificationRules",
                    "M24PlusJCI");
            return assessmentClassificationService.readAssessmentClassificationByCode(
                "journalM24Plus");
        }

        return null;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM24(String category) {
        return null;
    }

    private boolean isRankConditionPassed(PublicationSeriesIndicator indicator,
                                          double topPercentage) {
        if (Objects.nonNull(indicator) && !indicator.getTextualValue().startsWith("N")) {
            var tokens = indicator.getTextualValue().split("/");
            var rank = Double.parseDouble(tokens[0]);
            var total = Double.parseDouble(tokens[1]);
            return (rank / total) <= topPercentage;
        }
        return false;
    }

    @Nullable
    private AssessmentClassification handlePercentileClassification(String classificationCode,
                                                                    String category,
                                                                    double topPercentage,
                                                                    String rulePrefix) {
        Map<String, PublicationSeriesIndicator> indicators = new LinkedHashMap<>();
        Optional.ofNullable(findIndicatorByCode("currentJIFRank", category))
            .ifPresent(i -> indicators.put("IF2", i));
        Optional.ofNullable(findIndicatorByCode("fiveYearJIFRank", category))
            .ifPresent(i -> indicators.put("IF5", i));
        Optional.ofNullable(findIndicatorByCode("jciPercentile", null))
            .ifPresent(i -> indicators.put("JCI", i));

        for (var entry : indicators.entrySet()) {
            var suffix = entry.getKey();
            var indicator = entry.getValue();
            boolean conditionPassed =
                suffix.equals("JCI") ? isJciConditionPassed(indicator, topPercentage)
                    : isRankConditionPassed(indicator, topPercentage);

            if (conditionPassed) {
                var ruleCode = rulePrefix + suffix;
                var rank = suffix.equals("JCI") ? String.valueOf(indicator.getNumericValue()) :
                    indicator.getTextualValue();

                reasoningProcess = AssessmentRulesConfigurationLoader.getRuleDescription(
                    "journalClassificationRules", ruleCode, rank);

                return assessmentClassificationService.readAssessmentClassificationByCode(
                    classificationCode);
            }
        }

        return null;
    }


    private boolean isJciConditionPassed(PublicationSeriesIndicator jci, double topPercentage) {
        return Objects.nonNull(jci) && Objects.nonNull(jci.getNumericValue()) &&
            (100.00 - jci.getNumericValue()) <= (topPercentage * 100);
    }
}
