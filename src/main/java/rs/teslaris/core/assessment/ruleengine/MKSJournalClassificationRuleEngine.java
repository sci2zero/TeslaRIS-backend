package rs.teslaris.core.assessment.ruleengine;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.EntityIndicatorSource;
import rs.teslaris.core.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.assessment.util.AssessmentRulesConfigurationLoader;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.repository.document.JournalRepository;

public class MKSJournalClassificationRuleEngine extends JournalClassificationRuleEngine {
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
        this.source = EntityIndicatorSource.MKS_SLAVISTS;
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
        var mksCategory = findIndicatorByCode("slavistiCategory", category);

        if (Objects.nonNull(mksCategory) && mksCategory.getTextualValue().equals("I kategorija")) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("journalClassificationRules",
                    "m23MKS");
            return assessmentClassificationService.readAssessmentClassificationByCode("journalM23");
        }

        return null;
    }

    @Nullable
    @Override
    protected AssessmentClassification handleM23e(String category) {
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
