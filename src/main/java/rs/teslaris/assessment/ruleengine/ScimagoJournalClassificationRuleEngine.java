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

public class ScimagoJournalClassificationRuleEngine extends JournalClassificationRuleEngine {

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
        return null;
    }

    @Nullable
    @Override
    public AssessmentClassification handleM23(String category) {
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
        var sjr = findIndicatorByCode("sjr", category);

        if (Objects.nonNull(sjr) &&
            (sjr.getTextualValue().equals("Q1") || sjr.getTextualValue().equals("Q2") ||
                sjr.getTextualValue().equals("Q3"))) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("journalClassificationRules",
                    "M24PlusSJR", this.classificationYear, category);
            return assessmentClassificationService.readAssessmentClassificationByCode(
                "journalM24Plus");
        }

        return null;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM24(String category) {
        var sjr = findIndicatorByCode("sjr", category);

        if (Objects.nonNull(sjr) && sjr.getTextualValue().equals("Q4")) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("journalClassificationRules",
                    "M24SJR", this.classificationYear, category);
            return assessmentClassificationService.readAssessmentClassificationByCode("journalM24");
        }

        return null;
    }
}
