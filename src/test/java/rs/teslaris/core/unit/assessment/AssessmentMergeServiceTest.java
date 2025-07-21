package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import rs.teslaris.assessment.model.classification.EventAssessmentClassification;
import rs.teslaris.assessment.model.classification.PublicationSeriesAssessmentClassification;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.assessment.model.indicator.EventIndicator;
import rs.teslaris.assessment.model.indicator.Indicator;
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
import rs.teslaris.assessment.service.impl.AssessmentMergeServiceImpl;
import rs.teslaris.assessment.util.IndicatorMappingConfigurationLoader;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@SpringBootTest
public class AssessmentMergeServiceTest {

    @Mock
    private JournalService journalService;

    @Mock
    private BookSeriesService bookSeriesService;

    @Mock
    private ConferenceService conferenceService;

    @Mock
    private PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    @Mock
    private PublicationSeriesAssessmentClassificationRepository
        publicationSeriesAssessmentClassificationRepository;

    @Mock
    private EventIndicatorRepository eventIndicatorRepository;

    @Mock
    private EventAssessmentClassificationRepository eventAssessmentClassificationRepository;

    @Mock
    private PersonService personService;

    @Mock
    private PersonIndicatorRepository personIndicatorRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private DocumentIndicatorRepository documentIndicatorRepository;

    @InjectMocks
    private AssessmentMergeServiceImpl mergeService;


    @Test
    public void shouldSwitchAllIndicatorsToOtherJournal() {
        // given
        var sourceId = 1;
        var targetId = 2;

        var sourceJournalIndicator = new PublicationSeriesIndicator();
        sourceJournalIndicator.setPublicationSeries(new Journal());
        sourceJournalIndicator.setIndicator(new Indicator());

        var targetJournal = new Journal();
        when(journalService.findJournalById(targetId)).thenReturn(targetJournal);

        var indicatorsPage = List.of(sourceJournalIndicator);
        when(publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeries(eq(sourceId),
            any()))
            .thenReturn(new PageImpl<>(indicatorsPage));

        // when
        mergeService.switchAllIndicatorsToOtherJournal(sourceId, targetId);

        // then
        verify(journalService).findJournalById(targetId);
        verify(publicationSeriesIndicatorRepository).findIndicatorsForPublicationSeries(
            eq(sourceId), any());
        assertEquals(targetJournal, sourceJournalIndicator.getPublicationSeries());
    }

    @Test
    void shouldSwitchAllClassificationsToOtherJournal() {
        var sourceId = 1;
        var targetId = 2;
        var sourceClassification = new PublicationSeriesAssessmentClassification();
        sourceClassification.setCommission(new Commission());
        var targetJournal = new Journal();

        when(journalService.findJournalById(targetId)).thenReturn(targetJournal);
        when(
            publicationSeriesAssessmentClassificationRepository.findClassificationForPublicationSeriesAndCategoryAndYearAndCommission(
                eq(targetId), any(), any(), any()
            )).thenReturn(Optional.empty());

        var classificationsPage = List.of(sourceClassification);
        when(
            publicationSeriesAssessmentClassificationRepository.findClassificationsForPublicationSeries(
                eq(sourceId), any()))
            .thenReturn(new PageImpl<>(classificationsPage));

        mergeService.switchAllClassificationsToOtherJournal(sourceId, targetId);

        verify(journalService).findJournalById(targetId);
        verify(
            publicationSeriesAssessmentClassificationRepository).findClassificationsForPublicationSeries(
            eq(sourceId), any());
        assertEquals(targetJournal, sourceClassification.getPublicationSeries());
    }

    @Test
    void shouldSwitchAllIndicatorsToOtherEvent() {
        var sourceId = 1;
        var targetId = 2;
        var sourceIndicator = new EventIndicator();
        sourceIndicator.setIndicator(new Indicator());
        var targetEvent = new Conference();

        when(conferenceService.findConferenceById(targetId)).thenReturn(targetEvent);
        when(eventIndicatorRepository.existsByEventIdAndSourceAndYear(eq(targetId), any(), any(),
            any()))
            .thenReturn(Optional.empty());

        var indicatorsPage = List.of(sourceIndicator);
        when(eventIndicatorRepository.findIndicatorsForEvent(eq(sourceId), any()))
            .thenReturn(new PageImpl<>(indicatorsPage));

        mergeService.switchAllIndicatorsToOtherEvent(sourceId, targetId);

        verify(conferenceService).findConferenceById(targetId);
        verify(eventIndicatorRepository).findIndicatorsForEvent(eq(sourceId), any());
        assertEquals(targetEvent, sourceIndicator.getEvent());
    }

    @Test
    void shouldSwitchAllClassificationsToOtherEvent() {
        var sourceId = 1;
        var targetId = 2;
        var sourceClassification = new EventAssessmentClassification();
        sourceClassification.setCommission(new Commission());
        var targetEvent = new Conference();

        when(conferenceService.findConferenceById(targetId)).thenReturn(targetEvent);
        when(
            eventAssessmentClassificationRepository.findAssessmentClassificationsForEventAndCommissionAndYear(
                eq(targetId), any(), any()
            )).thenReturn(Optional.empty());

        var classificationsPage = List.of(sourceClassification);
        when(eventAssessmentClassificationRepository.findAssessmentClassificationsForEvent(
            eq(sourceId), any()))
            .thenReturn(new PageImpl<>(classificationsPage));

        mergeService.switchAllClassificationsToOtherEvent(sourceId, targetId);

        verify(conferenceService).findConferenceById(targetId);
        verify(eventAssessmentClassificationRepository).findAssessmentClassificationsForEvent(
            eq(sourceId), any());
        assertEquals(targetEvent, sourceClassification.getEvent());
    }

    @Test
    public void shouldSwitchAllIndicatorsToOtherPerson() {
        // given
        var sourceId = 1;
        var targetId = 2;

        var indicator = new Indicator();
        indicator.setCode("CODE1");

        var sourceIndicator = new PersonIndicator();
        sourceIndicator.setIndicator(indicator);
        sourceIndicator.setPerson(new Person());

        var targetPerson = new Person();
        when(personService.findOne(targetId)).thenReturn(targetPerson);

        try (var mocked = mockStatic(IndicatorMappingConfigurationLoader.class)) {
            mocked.when(() -> IndicatorMappingConfigurationLoader.isStatisticIndicatorCode("CODE1"))
                .thenReturn(false);
            when(personIndicatorRepository.findIndicatorsForPersonAndIndicatorAccessLevel(
                eq(sourceId),
                any()))
                .thenReturn(List.of(sourceIndicator));

            when(personIndicatorRepository.findIndicatorForCodeAndPersonId("CODE1", targetId))
                .thenReturn(Optional.empty());

            // when
            mergeService.switchAllIndicatorsToOtherPerson(sourceId, targetId);

            // then
            verify(personService).findOne(targetId);
            verify(personIndicatorRepository).findIndicatorsForPersonAndIndicatorAccessLevel(
                eq(sourceId), any());
            assertEquals(targetPerson, sourceIndicator.getPerson());
        }
    }

    @Test
    public void shouldSwitchAllIndicatorsToOtherOrganisationUnit() {
        // given
        Integer sourceId = 1;
        Integer targetId = 2;

        var indicator = new Indicator();
        indicator.setCode("ORG_INDICATOR");

        var sourceIndicator = new OrganisationUnitIndicator();
        sourceIndicator.setIndicator(indicator);
        sourceIndicator.setOrganisationUnit(new OrganisationUnit());

        var targetOU = new OrganisationUnit();
        when(organisationUnitService.findOne(targetId)).thenReturn(targetOU);

        try (var mocked = mockStatic(IndicatorMappingConfigurationLoader.class)) {
            mocked.when(
                    () -> IndicatorMappingConfigurationLoader.isStatisticIndicatorCode("ORG_INDICATOR"))
                .thenReturn(false);
            when(
                organisationUnitIndicatorRepository.findIndicatorsForOrganisationUnitAndIndicatorAccessLevel(
                    eq(sourceId), any()))
                .thenReturn(List.of(sourceIndicator));

            when(organisationUnitIndicatorRepository.findIndicatorForCodeAndOrganisationUnitId(
                "ORG_INDICATOR", targetId))
                .thenReturn(Optional.empty());

            // when
            mergeService.switchAllIndicatorsToOtherOrganisationUnit(sourceId, targetId);

            // then
            verify(organisationUnitService).findOne(targetId);
            verify(
                organisationUnitIndicatorRepository).findIndicatorsForOrganisationUnitAndIndicatorAccessLevel(
                eq(sourceId), any());
            assertEquals(targetOU, sourceIndicator.getOrganisationUnit());
        }
    }

    @Test
    public void shouldSwitchAllIndicatorsToOtherDocument() {
        // given
        Integer sourceId = 1;
        Integer targetId = 2;

        var indicator = new Indicator();
        indicator.setCode("DOC_INDICATOR");

        var sourceIndicator = new DocumentIndicator();
        sourceIndicator.setIndicator(indicator);
        sourceIndicator.setDocument(new Dataset());

        var targetDoc = new Dataset();
        when(documentPublicationService.findOne(targetId)).thenReturn(targetDoc);

        try (var mocked = mockStatic(IndicatorMappingConfigurationLoader.class)) {
            mocked.when(
                    () -> IndicatorMappingConfigurationLoader.isStatisticIndicatorCode("DOC_INDICATOR"))
                .thenReturn(false);
            when(documentIndicatorRepository.findIndicatorsForDocumentAndIndicatorAccessLevel(
                eq(sourceId), any()))
                .thenReturn(List.of(sourceIndicator));

            when(documentIndicatorRepository.findIndicatorForCodeAndDocumentId("DOC_INDICATOR",
                targetId))
                .thenReturn(Optional.empty());

            // when
            mergeService.switchAllIndicatorsToOtherDocument(sourceId, targetId);

            // then
            verify(documentPublicationService).findOne(targetId);
            verify(documentIndicatorRepository).findIndicatorsForDocumentAndIndicatorAccessLevel(
                eq(sourceId), any());
            assertEquals(targetDoc, sourceIndicator.getDocument());
        }
    }

    @Test
    public void shouldSwitchAllIndicatorsToOtherBookSeries() {
        // given
        var sourceId = 1;
        var targetId = 2;

        var sourceBookSeriesIndicator = new PublicationSeriesIndicator();
        sourceBookSeriesIndicator.setPublicationSeries(new Journal());
        sourceBookSeriesIndicator.setIndicator(new Indicator());

        var targetBookSeries = new BookSeries();
        when(bookSeriesService.findBookSeriesById(targetId)).thenReturn(targetBookSeries);

        var indicatorsPage = List.of(sourceBookSeriesIndicator);
        when(publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeries(eq(sourceId),
            any()))
            .thenReturn(new PageImpl<>(indicatorsPage));

        // when
        mergeService.switchAllIndicatorsToOtherBookSeries(sourceId, targetId);

        // then
        verify(bookSeriesService).findBookSeriesById(targetId);
        verify(publicationSeriesIndicatorRepository).findIndicatorsForPublicationSeries(
            eq(sourceId), any());
        assertEquals(targetBookSeries, sourceBookSeriesIndicator.getPublicationSeries());
    }
}
