package rs.teslaris.core.assessment.ruleengine;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.EntityIndicatorSource;
import rs.teslaris.core.assessment.model.PublicationSeriesIndicator;
import rs.teslaris.core.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
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
        return handlePercentileClassification("journalM21APlus", category, 0.05);
    }

    @Nullable
    @Override
    public AssessmentClassification handleM21A(String category) {
        return handlePercentileClassification("journalM21A", category, 0.15);
    }

    @Nullable
    @Override
    public AssessmentClassification handleM21(String category) {
        return handlePercentileClassification("journalM21", category, 0.35);
    }

    @Nullable
    @Override
    public AssessmentClassification handleM22(String category) {
        return handlePercentileClassification("journalM22", category, 0.75);
    }

    @Nullable
    @Override
    public AssessmentClassification handleM23(String category) {
        return handlePercentileClassification("journalM23", category, 1.0);
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
                                                                    double topPercentage) {
        var jci = findIndicatorByCode("jciPercentile", null);
        var jif2 = findIndicatorByCode("currentJIFRank", category);
        var jif5 = findIndicatorByCode("fiveYearJIFRank", category);

        var jif2ConditionPassed = isRankConditionPassed(jif2, topPercentage);
        var jif5ConditionPassed = isRankConditionPassed(jif5, topPercentage);
        var jciConditionPassed = isJciConditionPassed(jci, topPercentage);

        if (jif2ConditionPassed || jif5ConditionPassed || jciConditionPassed) {
            return assessmentClassificationService.readAssessmentClassificationByCode(
                classificationCode);
        }

        return null;
    }

    private boolean isJciConditionPassed(PublicationSeriesIndicator jci, double topPercentage) {
        return Objects.nonNull(jci) && Objects.nonNull(jci.getNumericValue()) &&
            (100.00 - jci.getNumericValue()) <= (topPercentage * 100);
    }
}
