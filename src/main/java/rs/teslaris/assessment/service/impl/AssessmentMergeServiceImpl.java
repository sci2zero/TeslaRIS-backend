package rs.teslaris.assessment.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.classification.EventAssessmentClassification;
import rs.teslaris.assessment.model.classification.PublicationSeriesAssessmentClassification;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.assessment.model.indicator.EventIndicator;
import rs.teslaris.assessment.model.indicator.OrganisationUnitIndicator;
import rs.teslaris.assessment.model.indicator.PersonIndicator;
import rs.teslaris.assessment.model.indicator.PublicationSeriesIndicator;
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
        var indicatorsToDelete = new ArrayList<PublicationSeriesIndicator>();

        processChunks(
            sourceId,
            (srcId, journalIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        journalIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    if (publicationSeriesIndicatorRepository.existsByPublicationSeriesIdAndSourceAndYearAndCategory(
                        targetId, journalIndicator.getSource(), journalIndicator.getFromDate(),
                        journalIndicator.getCategoryIdentifier(),
                        journalIndicator.getIndicator().getCode()).isPresent()) {
                        indicatorsToDelete.add(journalIndicator);
                        return;
                    }

                    journalIndicator.setPublicationSeries(journalService.findJournalById(targetId));
                }
            },
            pageRequest -> publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeries(
                sourceId, pageRequest).getContent()
        );

        publicationSeriesIndicatorRepository.deleteAll(indicatorsToDelete);
    }

    @Override
    public void switchAllClassificationsToOtherJournal(Integer sourceId, Integer targetId) {
        var classificationsToDelete = new ArrayList<PublicationSeriesAssessmentClassification>();

        processChunks(
            sourceId,
            (srcId, journalClassification) -> {
                if (publicationSeriesAssessmentClassificationRepository.findClassificationForPublicationSeriesAndCategoryAndYearAndCommission(
                    targetId, journalClassification.getCategoryIdentifier(),
                    journalClassification.getClassificationYear(),
                    journalClassification.getCommission().getId()).isPresent()) {
                    classificationsToDelete.add(journalClassification);
                    return;
                }

                journalClassification.setPublicationSeries(
                    journalService.findJournalById(targetId));
            },
            pageRequest -> publicationSeriesAssessmentClassificationRepository.findClassificationsForPublicationSeries(
                sourceId, pageRequest).getContent()
        );

        publicationSeriesAssessmentClassificationRepository.deleteAll(classificationsToDelete);
    }

    @Override
    public void switchAllIndicatorsToOtherEvent(Integer sourceId, Integer targetId) {
        var indicatorsToDelete = new ArrayList<EventIndicator>();

        processChunks(
            sourceId,
            (srcId, eventIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        eventIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    if (eventIndicatorRepository.existsByEventIdAndSourceAndYear(
                        targetId, eventIndicator.getSource(), eventIndicator.getFromDate(),
                        eventIndicator.getIndicator().getCode()).isPresent()) {
                        indicatorsToDelete.add(eventIndicator);
                        return;
                    }

                    eventIndicator.setEvent(conferenceService.findConferenceById(targetId));
                }
            },
            pageRequest -> eventIndicatorRepository.findIndicatorsForEvent(sourceId, pageRequest)
                .getContent()
        );

        eventIndicatorRepository.deleteAll(indicatorsToDelete);
    }

    @Override
    public void switchAllClassificationsToOtherEvent(Integer sourceId, Integer targetId) {
        var classificationsToDelete = new ArrayList<EventAssessmentClassification>();

        processChunks(
            sourceId,
            (srcId, eventClassification) -> {
                var existingClassificationValue =
                    eventAssessmentClassificationRepository.findAssessmentClassificationsForEventAndCommissionAndYear(
                        targetId, eventClassification.getCommission().getId(),
                        eventClassification.getClassificationYear());
                if (existingClassificationValue.isPresent()) {
                    classificationsToDelete.add(eventClassification);
                    return;
                }

                eventClassification.setEvent(conferenceService.findConferenceById(targetId));
            },
            pageRequest -> eventAssessmentClassificationRepository.findAssessmentClassificationsForEvent(
                sourceId, pageRequest).getContent()
        );

        eventAssessmentClassificationRepository.deleteAll(classificationsToDelete);
    }

    @Override
    public void switchAllIndicatorsToOtherPerson(Integer sourceId, Integer targetId) {
        var indicatorsToDelete = new ArrayList<PersonIndicator>();

        processChunks(
            sourceId,
            (srcId, personIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        personIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    if (personIndicatorRepository.indicatorsExistForCodeAndPersonId(
                        personIndicator.getIndicator().getCode(), targetId)) {
                        indicatorsToDelete.add(personIndicator);
                        return;
                    }

                    personIndicator.setPerson(personService.findOne(targetId));
                }
            },
            pageRequest -> personIndicatorRepository.findIndicatorsForPersonAndIndicatorAccessLevel(
                sourceId, AccessLevel.ADMIN_ONLY)
        );

        personIndicatorRepository.deleteAll(indicatorsToDelete);
    }

    @Override
    public void switchAllIndicatorsToOtherOrganisationUnit(Integer sourceId, Integer targetId) {
        var indicatorsToDelete = new ArrayList<OrganisationUnitIndicator>();

        processChunks(
            sourceId,
            (srcId, organisationUnitIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        organisationUnitIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    if (organisationUnitIndicatorRepository.findIndicatorForCodeAndOrganisationUnitId(
                        organisationUnitIndicator.getIndicator().getCode(), targetId).isPresent()) {
                        indicatorsToDelete.add(organisationUnitIndicator);
                        return;
                    }

                    organisationUnitIndicator.setOrganisationUnit(
                        organisationUnitService.findOne(targetId));
                }
            },
            pageRequest -> organisationUnitIndicatorRepository.findIndicatorsForOrganisationUnitAndIndicatorAccessLevel(
                sourceId, AccessLevel.ADMIN_ONLY)
        );

        organisationUnitIndicatorRepository.deleteAll(indicatorsToDelete);
    }

    @Override
    public void switchAllIndicatorsToOtherDocument(Integer sourceId, Integer targetId) {
        var indicatorsToDelete = new ArrayList<DocumentIndicator>();

        processChunks(
            sourceId,
            (srcId, documentIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        documentIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    if (documentIndicatorRepository.findIndicatorForCodeAndDocumentId(
                        documentIndicator.getIndicator().getCode(), targetId).isPresent()) {
                        indicatorsToDelete.add(documentIndicator);
                        return;
                    }

                    documentIndicator.setDocument(documentPublicationService.findOne(targetId));
                }
            },
            pageRequest -> documentIndicatorRepository.findIndicatorsForDocumentAndIndicatorAccessLevel(
                sourceId, AccessLevel.ADMIN_ONLY)
        );

        documentIndicatorRepository.deleteAll(indicatorsToDelete);
    }

    @Override
    public void switchAllIndicatorsToOtherBookSeries(Integer sourceId, Integer targetId) {
        var indicatorsToDelete = new ArrayList<PublicationSeriesIndicator>();

        processChunks(
            sourceId,
            (srcId, bookSeriesIndicator) -> {
                var isStatisticIndicator =
                    IndicatorMappingConfigurationLoader.isStatisticIndicatorCode(
                        bookSeriesIndicator.getIndicator().getCode());

                if (!isStatisticIndicator) {
                    if (publicationSeriesIndicatorRepository.existsByPublicationSeriesIdAndSourceAndYearAndCategory(
                        targetId, bookSeriesIndicator.getSource(),
                        bookSeriesIndicator.getFromDate(),
                        bookSeriesIndicator.getCategoryIdentifier(),
                        bookSeriesIndicator.getIndicator().getCode()).isPresent()) {
                        indicatorsToDelete.add(bookSeriesIndicator);
                        return;
                    }


                    bookSeriesIndicator.setPublicationSeries(
                        bookSeriesService.findBookSeriesById(targetId));
                }
            },
            pageRequest -> publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeries(
                sourceId, pageRequest).getContent()
        );

        publicationSeriesIndicatorRepository.deleteAll(indicatorsToDelete);
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
