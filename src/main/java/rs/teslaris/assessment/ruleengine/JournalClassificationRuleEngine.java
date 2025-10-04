package rs.teslaris.assessment.ruleengine;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.assessment.model.classification.PublicationSeriesAssessmentClassification;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.model.indicator.PublicationSeriesIndicator;
import rs.teslaris.assessment.repository.classification.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.indicator.PublicationSeriesIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.util.functional.Pair;

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
        int chunkSize = 500;
        boolean hasNextPage = true;

        this.classificationYear = classificationYear;

        var batchClassifications = new ArrayList<PublicationSeriesAssessmentClassification>();

        while (hasNextPage) {
            List<JournalIndex> chunk =
                journalIndexRepository.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((journalIndex) -> {
                this.currentJournal =
                    journalRepository.getReferenceById(journalIndex.getDatabaseId());
                this.currentJournalIndicators =
                    publicationSeriesIndicatorRepository.findCombinedIndicatorsForPublicationSeriesAndIndicatorSourceAndYear(
                        journalIndex.getDatabaseId(), classificationYear, source);

                performClassification(commission, batchClassifications);
                reasoningProcess.clear();
            });

            assessmentClassificationRepository.saveAll(batchClassifications);
            batchClassifications.clear();

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    @Transactional
    public void startClassification(Integer classificationYear, Commission commission,
                                    List<Integer> journalIds) {
        this.classificationYear = classificationYear;
        var batchClassifications = new ArrayList<PublicationSeriesAssessmentClassification>();

        journalIds.forEach((journalId) -> {
            var journalIndexOptional =
                journalIndexRepository.findJournalIndexByDatabaseId(journalId);
            journalIndexOptional.ifPresent(journalIndex -> {
                this.currentJournal =
                    journalRepository.getReferenceById(journalIndex.getDatabaseId());
                this.currentJournalIndicators =
                    publicationSeriesIndicatorRepository.findCombinedIndicatorsForPublicationSeriesAndIndicatorSourceAndYear(
                        journalIndex.getDatabaseId(), classificationYear, source);

                performClassification(commission, batchClassifications);
            });
        });

        assessmentClassificationRepository.saveAll(batchClassifications);
        batchClassifications.clear();
    }

    @Transactional
    private void performClassification(Commission commission,
                                       ArrayList<PublicationSeriesAssessmentClassification> batchClassifications) {
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
                    new Pair<>(assessmentClassification, categoryIdentifier),
                    batchClassifications)) {
                    break;
                }
            }
        }
    }

    @Transactional
    private boolean saveIfNotNull(PublicationSeriesAssessmentClassification classification,
                                  Pair<AssessmentClassification, String> assessmentClassification,
                                  ArrayList<PublicationSeriesAssessmentClassification> batchClassifications) {
        if (Objects.nonNull(assessmentClassification.a)) {
            classification.setCategoryIdentifier(assessmentClassification.b);
            classification.setAssessmentClassification(assessmentClassification.a);
            classification.setClassificationReason(reasoningProcess);

            var existingClassification =
                assessmentClassificationRepository.findClassificationForPublicationSeriesAndCategoryAndYearAndCommission(
                    classification.getPublicationSeries().getId(), assessmentClassification.b,
                    classificationYear, classification.getCommission().getId());
            existingClassification.ifPresent(assessmentClassificationRepository::delete);

            batchClassifications.add(classification);
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
