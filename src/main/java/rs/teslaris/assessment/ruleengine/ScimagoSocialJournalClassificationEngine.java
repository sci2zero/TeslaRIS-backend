package rs.teslaris.assessment.ruleengine;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.repository.classification.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.indicator.PublicationSeriesIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.assessment.util.AssessmentRulesConfigurationLoader;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.repository.document.JournalRepository;

public class ScimagoSocialJournalClassificationEngine extends JournalClassificationRuleEngine {

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
        this.source = EntityIndicatorSource.SCIMAGO;
    }

    @Nullable
    @Override
    public AssessmentClassification handleM21APlus(String category) {
        return null;
    }

    @Nullable
    @Override
    public AssessmentClassification handleM21A(String category) {
        return null;
    }

    @Nullable
    @Override
    public AssessmentClassification handleM21(String category) {
        return null;
    }

    @Nullable
    @Override
    public AssessmentClassification handleM22(String category) {
        var sjr = findIndicatorByCode("sjr", category);

        if (Objects.nonNull(sjr) && sjr.getTextualValue().equals("Q1")) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("journalClassificationRules",
                    "M22SJR", this.classificationYear, category);
            return assessmentClassificationService.readAssessmentClassificationByCode("journalM22");
        }

        return null;
    }

    @Nullable
    @Override
    public AssessmentClassification handleM23(String category) {
        var sjr = findIndicatorByCode("sjr", category);

        if (Objects.nonNull(sjr) && sjr.getTextualValue().equals("Q2")) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("journalClassificationRules",
                    "m23SJR", this.classificationYear, category);
            return assessmentClassificationService.readAssessmentClassificationByCode("journalM23");
        }

        return null;
    }

    @Nullable
    @Override
    public AssessmentClassification handleM23e(String category) {
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
