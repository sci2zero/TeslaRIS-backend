package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import rs.teslaris.assessment.model.EventAssessmentClassification;
import rs.teslaris.assessment.model.EventIndicator;
import rs.teslaris.assessment.model.Indicator;
import rs.teslaris.assessment.model.PublicationSeriesAssessmentClassification;
import rs.teslaris.assessment.model.PublicationSeriesIndicator;
import rs.teslaris.assessment.repository.EventAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.EventIndicatorRepository;
import rs.teslaris.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.assessment.service.impl.AssessmentMergeServiceImpl;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.JournalService;

@SpringBootTest
public class AssessmentMergeServiceTest {

    @Mock
    private JournalService journalService;

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
}
