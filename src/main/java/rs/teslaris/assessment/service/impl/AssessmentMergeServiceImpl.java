package rs.teslaris.assessment.service.impl;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.repository.classification.EventAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.indicator.DocumentIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.EventIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.OrganisationUnitIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.PersonIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.PublicationSeriesIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.AssessmentMergeService;
import rs.teslaris.assessment.util.IndicatorMappingConfigurationLoader;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class AssessmentMergeServiceImpl implements AssessmentMergeService {

    private final JournalService journalService;

    private final BookSeriesService bookSeriesService;

    private final ConferenceService conferenceService;

    private final EventIndicatorRepository eventIndicatorRepository;

    private final EventAssessmentClassificationRepository eventAssessmentClassificationRepository;

    private final PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    private final PublicationSeriesAssessmentClassificationRepository
        publicationSeriesAssessmentClassificationRepository;

    private final PersonService personService;

    private final PersonIndicatorRepository personIndicatorRepository;

    private final OrganisationUnitService organisationUnitService;

    private final OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository;

    private final DocumentPublicationService documentPublicationService;

    private final DocumentIndicatorRepository documentIndicatorRepository;


    @Override
    public void switchAllIndicatorsToOtherJournal(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, journalIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        journalIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    publicationSeriesIndicatorRepository.deleteByPublicationSeriesIdAndSourceAndYearAndCategory(
                        targetId, journalIndicator.getSource(), journalIndicator.getFromDate(),
                        journalIndicator.getCategoryIdentifier(),
                        journalIndicator.getIndicator().getCode());

                    journalIndicator.setPublicationSeries(journalService.findJournalById(targetId));
                }
            },
            pageRequest -> publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeries(
                sourceId, pageRequest).getContent()
        );
    }

    @Override
    public void switchAllClassificationsToOtherJournal(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, journalClassification) -> {
                var existingClassificationValue =
                    publicationSeriesAssessmentClassificationRepository.findClassificationForPublicationSeriesAndCategoryAndYearAndCommission(
                        targetId, journalClassification.getCategoryIdentifier(),
                        journalClassification.getClassificationYear(),
                        journalClassification.getCommission().getId());
                existingClassificationValue.ifPresent(
                    publicationSeriesAssessmentClassificationRepository::delete);

                journalClassification.setPublicationSeries(
                    journalService.findJournalById(targetId));
            },
            pageRequest -> publicationSeriesAssessmentClassificationRepository.findClassificationsForPublicationSeries(
                sourceId, pageRequest).getContent()
        );
    }

    @Override
    public void switchAllIndicatorsToOtherEvent(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, eventIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        eventIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    eventIndicatorRepository.deleteByEventIdAndSourceAndYear(
                        targetId, eventIndicator.getSource(), eventIndicator.getFromDate(),
                        eventIndicator.getIndicator().getCode());

                    eventIndicator.setEvent(conferenceService.findConferenceById(targetId));
                }
            },
            pageRequest -> eventIndicatorRepository.findIndicatorsForEvent(sourceId, pageRequest)
                .getContent()
        );
    }

    @Override
    public void switchAllClassificationsToOtherEvent(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, eventClassification) -> {
                var existingClassificationValue =
                    eventAssessmentClassificationRepository.findAssessmentClassificationsForEventAndCommissionAndYear(
                        targetId, eventClassification.getCommission().getId(),
                        eventClassification.getClassificationYear());
                existingClassificationValue.ifPresent(
                    eventAssessmentClassificationRepository::delete);

                eventClassification.setEvent(conferenceService.findConferenceById(targetId));
            },
            pageRequest -> eventAssessmentClassificationRepository.findAssessmentClassificationsForEvent(
                sourceId, pageRequest).getContent()
        );
    }

    @Override
    public void switchAllIndicatorsToOtherPerson(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, personIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        personIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    personIndicatorRepository.deleteIndicatorsForCodeAndPersonId(
                        personIndicator.getIndicator().getCode(), targetId);

                    personIndicator.setPerson(personService.findOne(targetId));
                }
            },
            pageRequest -> personIndicatorRepository.findIndicatorsForPersonAndIndicatorAccessLevel(
                sourceId, AccessLevel.ADMIN_ONLY)
        );
    }

    @Override
    public void switchAllIndicatorsToOtherOrganisationUnit(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, organisationUnitIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        organisationUnitIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    organisationUnitIndicatorRepository.deleteIndicatorsForCodeAndOrganisationUnitId(
                        organisationUnitIndicator.getIndicator().getCode(), targetId);

                    organisationUnitIndicator.setOrganisationUnit(
                        organisationUnitService.findOne(targetId));
                }
            },
            pageRequest -> organisationUnitIndicatorRepository.findIndicatorsForOrganisationUnitAndIndicatorAccessLevel(
                sourceId, AccessLevel.ADMIN_ONLY)
        );
    }

    @Override
    public void switchAllIndicatorsToOtherDocument(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, documentIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        documentIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    documentIndicatorRepository.deleteIndicatorsForCodeAndDocumentId(
                        documentIndicator.getIndicator().getCode(), targetId);

                    documentIndicator.setDocument(documentPublicationService.findOne(targetId));
                }
            },
            pageRequest -> documentIndicatorRepository.findIndicatorsForDocumentAndIndicatorAccessLevel(
                sourceId, AccessLevel.ADMIN_ONLY)
        );
    }

    @Override
    public void switchAllIndicatorsToOtherBookSeries(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, bookSeriesIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        bookSeriesIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    var existingIndicatorValue =
                        publicationSeriesIndicatorRepository.existsByPublicationSeriesIdAndSourceAndYearAndCategory(
                            targetId, bookSeriesIndicator.getSource(),
                            bookSeriesIndicator.getFromDate(),
                            bookSeriesIndicator.getCategoryIdentifier(),
                            bookSeriesIndicator.getIndicator().getCode());

                    existingIndicatorValue.ifPresent(publicationSeriesIndicatorRepository::delete);
                    bookSeriesIndicator.setPublicationSeries(
                        bookSeriesService.findBookSeriesById(targetId));
                }
            },
            pageRequest -> publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeries(
                sourceId, pageRequest).getContent()
        );
    }

    private <T> void processChunks(int sourceId,
                                   BiConsumer<Integer, T> switchOperation,
                                   Function<PageRequest, List<T>> fetchChunk) {
        var pageNumber = 0;
        var chunkSize = 100;
        var hasNextPage = true;

        while (hasNextPage) {
            List<T> chunk = fetchChunk.apply(PageRequest.of(pageNumber, chunkSize));

            chunk.forEach(item -> switchOperation.accept(sourceId, item));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }
}
