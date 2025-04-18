package rs.teslaris.assessment.ruleengine;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import rs.teslaris.assessment.model.AssessmentClassification;
import rs.teslaris.assessment.model.EntityIndicatorSource;
import rs.teslaris.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.assessment.util.AssessmentRulesConfigurationLoader;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.repository.document.JournalRepository;

public class ErihPlusJournalClassificationRuleEngine extends JournalClassificationRuleEngine {
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
        this.source = EntityIndicatorSource.ERIH_PLUS;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM21APlus(String category) {
        return null;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM21A(String category) {
        return null;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM21(String category) {
        return null;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM22(String category) {
        return null;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM23(String category) {
        return null;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM23e(String category) {
        var erihPlus = findIndicatorByCode("erihPlus", null);
        var erihPlusConditionPassed = Objects.nonNull(erihPlus) && erihPlus.getBooleanValue();

        if (erihPlusConditionPassed) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("journalClassificationRules",
                    "m23e", this.classificationYear, category);
            return assessmentClassificationService.readAssessmentClassificationByCode(
                "journalM23e");
        }

        return null;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM24plus(String category) {
        return null;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM24(String category) {
        return null;
    }
}
