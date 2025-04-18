package rs.teslaris.assessment.ruleengine;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.AssessmentClassification;
import rs.teslaris.assessment.model.Commission;
import rs.teslaris.assessment.model.EntityIndicatorSource;
import rs.teslaris.assessment.model.PublicationSeriesAssessmentClassification;
import rs.teslaris.assessment.model.PublicationSeriesIndicator;
import rs.teslaris.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
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

    @Getter
    protected Set<MultiLingualContent> reasoningProcess = new HashSet<>();


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
                    publicationSeriesIndicatorRepository.findCombinedIndicatorsForPublicationSeriesAndIndicatorSourceAndYear(
                        journalIndex.getDatabaseId(), classificationYear, source);

                performClassification(commission);
                reasoningProcess.clear();
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    @Transactional
    public void startClassification(Integer classificationYear, Commission commission,
                                    List<Integer> journalIds) {
        this.classificationYear = classificationYear;

        journalIds.forEach((journalId) -> {
            var journalIndexOptional =
                journalIndexRepository.findJournalIndexByDatabaseId(journalId);
            journalIndexOptional.ifPresent(journalIndex -> {
                this.currentJournal =
                    journalRepository.getReferenceById(journalIndex.getDatabaseId());
                this.currentJournalIndicators =
                    publicationSeriesIndicatorRepository.findCombinedIndicatorsForPublicationSeriesAndIndicatorSourceAndYear(
                        journalIndex.getDatabaseId(), classificationYear, source);

                performClassification(commission);
            });
        });
    }

    @Transactional
    private void performClassification(Commission commission) {
        List<Function<String, AssessmentClassification>> handlers = List.of(
            this::handleM21APlus,
            this::handleM21A,
            this::handleM21,
            this::handleM22,
            this::handleM23,
            this::handleM23e,
            this::handleM24plus,
            this::handleM24
        );

        var distinctCategoryIdentifiers = currentJournalIndicators.stream()
            .map(PublicationSeriesIndicator::getCategoryIdentifier)
            .filter(Objects::nonNull)
            .filter(categoryIdentifier -> !categoryIdentifier.isEmpty())
            .distinct()
            .toList();

        distinctCategoryIdentifiers = distinctCategoryIdentifiers.isEmpty()
            ? List.of("")
            : distinctCategoryIdentifiers;

        for (var categoryIdentifier : distinctCategoryIdentifiers) {
            var entityClassification = new PublicationSeriesAssessmentClassification();
            entityClassification.setPublicationSeries(currentJournal);
            entityClassification.setTimestamp(LocalDateTime.now());
            entityClassification.setCommission(commission);
            entityClassification.setClassificationYear(this.classificationYear);

            for (Function<String, AssessmentClassification> handler : handlers) {
                var assessmentClassification = handler.apply(categoryIdentifier);
                if (saveIfNotNull(entityClassification,
                    new Pair<>(assessmentClassification, categoryIdentifier))) {
                    break;
                }
            }
        }
    }

    @Transactional
    private boolean saveIfNotNull(PublicationSeriesAssessmentClassification classification,
                                  Pair<AssessmentClassification, String> assessmentClassification) {
        if (Objects.nonNull(assessmentClassification.a)) {
            classification.setCategoryIdentifier(assessmentClassification.b);
            classification.setAssessmentClassification(assessmentClassification.a);
            classification.setClassificationReason(reasoningProcess);

            var existingClassification =
                assessmentClassificationRepository.findClassificationForPublicationSeriesAndCategoryAndYearAndCommission(
                    classification.getPublicationSeries().getId(), assessmentClassification.b,
                    classificationYear, classification.getCommission().getId());
            existingClassification.ifPresent(assessmentClassificationRepository::delete);

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

    @Nullable
    abstract protected AssessmentClassification handleM24(String category);

    @Nullable
    protected PublicationSeriesIndicator findIndicatorByCode(String code, String category) {
        return currentJournalIndicators.stream()
            .filter(journalIndicator ->
                journalIndicator.getIndicator().getCode().equals(code) &&
                    journalIndicator.getFromDate().getYear() <= this.classificationYear &&
                    (category == null || category.equals(journalIndicator.getCategoryIdentifier())))
            .findFirst()
            .orElse(null);
    }
}
