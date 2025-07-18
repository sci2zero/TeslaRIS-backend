package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.assessment.dto.classification.EventAssessmentClassificationDTO;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.assessment.model.classification.EventAssessmentClassification;
import rs.teslaris.assessment.repository.classification.EventAssessmentClassificationRepository;
import rs.teslaris.assessment.service.impl.classification.EventAssessmentClassificationServiceImpl;
import rs.teslaris.assessment.service.impl.cruddelegate.EventAssessmentClassificationJPAServiceImpl;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.EventService;

@SpringBootTest
public class EventAssessmentClassificationServiceTest {

    @Mock
    private EventAssessmentClassificationJPAServiceImpl eventAssessmentClassificationJPAService;

    @Mock
    private EventAssessmentClassificationRepository eventAssessmentClassificationRepository;

    @Mock
    private AssessmentClassificationService assessmentClassificationService;

    @Mock
    private EventService eventService;

    @Mock
    private CommissionService commissionService;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private ConferenceService conferenceService;

    @InjectMocks
    private EventAssessmentClassificationServiceImpl eventAssessmentClassificationService;

    @Test
    void shouldReadAllEventAssessmentClassificationsForEvent() {
        // Given
        var eventId = 1;

        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setCode("code");

        var eventAssessmentClassification1 = new EventAssessmentClassification();
        eventAssessmentClassification1.setAssessmentClassification(assessmentClassification);
        eventAssessmentClassification1.setClassificationYear(2025);

        var eventAssessmentClassification2 = new EventAssessmentClassification();
        eventAssessmentClassification2.setAssessmentClassification(assessmentClassification);
        eventAssessmentClassification2.setClassificationYear(2025);

        when(
            eventAssessmentClassificationRepository.findAssessmentClassificationsForEvent(
                eventId)).thenReturn(
            List.of(eventAssessmentClassification1, eventAssessmentClassification2));

        // When
        var response =
            eventAssessmentClassificationService.getAssessmentClassificationsForEvent(eventId);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
    }

    @Test
    void shouldCreateEventAssessmentClassification() {
        var eventAssessmentClassificationDTO = new EventAssessmentClassificationDTO();
        eventAssessmentClassificationDTO.setEventId(1);
        eventAssessmentClassificationDTO.setAssessmentClassificationId(1);
        eventAssessmentClassificationDTO.setCommissionId(1);

        var newEventAssessmentClassification = new EventAssessmentClassification();
        newEventAssessmentClassification.setAssessmentClassification(
            new AssessmentClassification());

        var conference = new Conference();
        conference.setDateFrom(LocalDate.of(2020, 4, 2));
        when(eventService.findOne(1)).thenReturn(conference);

        when(commissionService.findOne(1)).thenReturn(new Commission());

        when(eventAssessmentClassificationJPAService.save(any(EventAssessmentClassification.class)))
            .thenReturn(newEventAssessmentClassification);
        when(assessmentClassificationService.findOne(1)).thenReturn(new AssessmentClassification());

        var result = eventAssessmentClassificationService.createEventAssessmentClassification(
            eventAssessmentClassificationDTO);

        assertNotNull(result);
        verify(eventAssessmentClassificationJPAService).save(
            any(EventAssessmentClassification.class));
    }

    @Test
    void shouldUpdateEventAssessmentClassification() {
        var eventAssessmentClassificationId = 1;
        var eventAssessmentClassificationDTO = new EventAssessmentClassificationDTO();
        eventAssessmentClassificationDTO.setEventId(1);
        eventAssessmentClassificationDTO.setAssessmentClassificationId(1);

        var existingEventAssessmentClassification = new EventAssessmentClassification();
        existingEventAssessmentClassification.setAssessmentClassification(
            new AssessmentClassification());
        existingEventAssessmentClassification.setCommission(new Commission());

        var event = new Conference();
        event.setDateFrom(LocalDate.now());

        when(eventAssessmentClassificationJPAService.findOne(
            eventAssessmentClassificationId)).thenReturn(
            existingEventAssessmentClassification);
        when(eventService.findOne(1)).thenReturn(event);
        when(assessmentClassificationService.findOne(1)).thenReturn(new AssessmentClassification());

        eventAssessmentClassificationService.updateEventAssessmentClassification(
            eventAssessmentClassificationId,
            eventAssessmentClassificationDTO);

        verify(eventAssessmentClassificationJPAService).findOne(eventAssessmentClassificationId);
        verify(eventAssessmentClassificationJPAService).save(existingEventAssessmentClassification);
    }
}
