package rs.teslaris.core.assessment.ruleengine;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.Commission;
import rs.teslaris.core.assessment.model.EntityIndicatorSource;
import rs.teslaris.core.assessment.model.PublicationSeriesAssessmentClassification;
import rs.teslaris.core.assessment.model.PublicationSeriesIndicator;
import rs.teslaris.core.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.util.Pair;

@Transactional
public abstract class JournalClassificationRuleEngine {

    protected PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    protected JournalRepository journalRepository;

    protected JournalIndexRepository journalIndexRepository;

    protected EntityIndicatorSource source;

    protected PublicationSeriesAssessmentClassificationRepository
        assessmentClassificationRepository;

    protected List<PublicationSeriesIndicator> currentJournalIndicators;

    protected Journal currentJournal;

    protected Integer classificationYear;

    protected AssessmentClassificationService assessmentClassificationService;

    @Transactional
    public void startClassification(Integer classificationYear, Commission commission) {
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        this.classificationYear = classificationYear;

        while (hasNextPage) {
            List<JournalIndex> chunk =
                journalIndexRepository.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((journalIndex) -> {
                this.currentJournal =
                    journalRepository.getReferenceById(journalIndex.getDatabaseId());
                this.currentJournalIndicators =
                    publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeriesAndIndicatorSourceAndYear(
                        journalIndex.getDatabaseId(), classificationYear, source);

                var classification = new PublicationSeriesAssessmentClassification();
                classification.setPublicationSeries(currentJournal);
                classification.setTimestamp(LocalDateTime.now());
                classification.setCommission(commission);

                performClassification(classification);
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    @Transactional
    private void performClassification(
        PublicationSeriesAssessmentClassification entityClassification) {
        List<Function<String, AssessmentClassification>> handlers = List.of(
            this::handleM21APlus,
            this::handleM21A,
            this::handleM21,
            this::handleM22,
            this::handleM23,
            this::handleM23e
        );

        var distinctCategoryIdentifiers = currentJournalIndicators.stream()
            .map(PublicationSeriesIndicator::getCategoryIdentifier)
            .filter(Objects::nonNull)
            .filter(categoryIdentifier -> !categoryIdentifier.isEmpty())
            .distinct()
            .toList();

        for (var categoryIdentifier : distinctCategoryIdentifiers) {
            for (Function<String, AssessmentClassification> handler : handlers) {
                var assessmentClassification = handler.apply(categoryIdentifier);
                if (saveIfNotNull(entityClassification,
                    new Pair<>(assessmentClassification, categoryIdentifier))) {
                    return;
                }
            }
        }
    }

    @Transactional
    private boolean saveIfNotNull(PublicationSeriesAssessmentClassification classification,
                                  Pair<AssessmentClassification, String> assessmentClassification) {
        if (Objects.nonNull(assessmentClassification)) {
            classification.setCategoryIdentifier(assessmentClassification.b);
            classification.setAssessmentClassification(assessmentClassification.a);
            assessmentClassificationRepository.save(classification);
            return true;
        }

        return false;
    }

    abstract public void initialize(
        PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository,
        JournalRepository journalRepository,
        JournalIndexRepository journalIndexRepository,
        PublicationSeriesAssessmentClassificationRepository assessmentClassificationRepository,
        AssessmentClassificationService assessmentClassificationService);

    @Nullable
    abstract protected AssessmentClassification handleM21APlus(String category);

    @Nullable
    abstract protected AssessmentClassification handleM21A(String category);

    @Nullable
    abstract protected AssessmentClassification handleM21(String category);

    @Nullable
    abstract protected AssessmentClassification handleM22(String category);

    @Nullable
    abstract protected AssessmentClassification handleM23(String category);

    @Nullable
    abstract protected AssessmentClassification handleM23e(String category);

    @Nullable
    abstract protected AssessmentClassification handleM24plus(String category);
}
