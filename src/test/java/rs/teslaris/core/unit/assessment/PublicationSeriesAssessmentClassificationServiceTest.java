package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.assessment.dto.classification.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.assessment.model.classification.PublicationSeriesAssessmentClassification;
import rs.teslaris.assessment.repository.classification.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.indicator.PublicationSeriesIndicatorRepository;
import rs.teslaris.assessment.service.impl.classification.PublicationSeriesAssessmentClassificationServiceImpl;
import rs.teslaris.assessment.service.impl.cruddelegate.PublicationSeriesAssessmentClassificationJPAServiceImpl;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;

@SpringBootTest
public class PublicationSeriesAssessmentClassificationServiceTest {

    @Mock
    private PublicationSeriesAssessmentClassificationJPAServiceImpl
        publicationSeriesAssessmentClassificationJPAService;

    @Mock
    private PublicationSeriesAssessmentClassificationRepository
        publicationSeriesAssessmentClassificationRepository;

    @Mock
    private AssessmentClassificationService assessmentClassificationService;

    @Mock
    private PublicationSeriesService publicationSeriesService;

    @Mock
    private PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private JournalIndexRepository journalIndexRepository;

    @Mock
    private CommissionService commissionService;

    @Mock
    private TaskManagerService taskManagerService;

    @Mock
    private JournalService journalService;

    @InjectMocks
    private PublicationSeriesAssessmentClassificationServiceImpl
        publicationSeriesAssessmentClassificationService;


    @Test
    void shouldReadAllPublicationSeriesAssessmentClassificationsForPublicationSeries() {
        // Given
        var publicationSeriesId = 1;

        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setCode("code");

        var publicationSeriesAssessmentClassification1 =
            new PublicationSeriesAssessmentClassification();
        publicationSeriesAssessmentClassification1.setAssessmentClassification(
            assessmentClassification);
        publicationSeriesAssessmentClassification1.setClassificationYear(2025);

        var publicationSeriesAssessmentClassification2 =
            new PublicationSeriesAssessmentClassification();
        publicationSeriesAssessmentClassification2.setAssessmentClassification(
            assessmentClassification);
        publicationSeriesAssessmentClassification2.setClassificationYear(2025);

        when(
            publicationSeriesAssessmentClassificationRepository.findAssessmentClassificationsForPublicationSeries(
                publicationSeriesId)).thenReturn(
            List.of(publicationSeriesAssessmentClassification1,
                publicationSeriesAssessmentClassification2));

        // When
        var response =
            publicationSeriesAssessmentClassificationService.getAssessmentClassificationsForPublicationSeries(
                publicationSeriesId);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
    }

    @Test
    void shouldCreatePublicationSeriesAssessmentClassification() {
        // given
        var publicationSeriesAssessmentClassificationDTO =
            new PublicationSeriesAssessmentClassificationDTO();
        publicationSeriesAssessmentClassificationDTO.setPublicationSeriesId(1);
        publicationSeriesAssessmentClassificationDTO.setAssessmentClassificationId(1);
        publicationSeriesAssessmentClassificationDTO.setClassificationYear(2025);

        var newPublicationSeriesAssessmentClassification =
            new PublicationSeriesAssessmentClassification();
        newPublicationSeriesAssessmentClassification.setAssessmentClassification(
            new AssessmentClassification());

        when(publicationSeriesService.findOne(1)).thenReturn(new Journal() {{
            setId(1);
        }});
        when(publicationSeriesAssessmentClassificationJPAService.save(
            any(PublicationSeriesAssessmentClassification.class)))
            .thenReturn(newPublicationSeriesAssessmentClassification);
        when(assessmentClassificationService.findOne(1)).thenReturn(new AssessmentClassification());

        // when
        var result =
            publicationSeriesAssessmentClassificationService.createPublicationSeriesAssessmentClassification(
                publicationSeriesAssessmentClassificationDTO);

        // then
        assertNotNull(result);
        verify(publicationSeriesAssessmentClassificationJPAService).save(
            any(PublicationSeriesAssessmentClassification.class));
        verify(journalService).reindexJournalVolatileInformation(anyInt());
    }

    @Test
    void shouldUpdatePublicationSeriesAssessmentClassification() {
        // given
        var publicationSeriesAssessmentClassificationId = 1;
        var publicationSeriesAssessmentClassificationDTO =
            new PublicationSeriesAssessmentClassificationDTO();
        publicationSeriesAssessmentClassificationDTO.setPublicationSeriesId(1);
        publicationSeriesAssessmentClassificationDTO.setAssessmentClassificationId(1);
        publicationSeriesAssessmentClassificationDTO.setClassificationYear(2025);

        var existingPublicationSeriesAssessmentClassification =
            new PublicationSeriesAssessmentClassification();
        existingPublicationSeriesAssessmentClassification.setAssessmentClassification(
            new AssessmentClassification());

        when(publicationSeriesAssessmentClassificationJPAService.findOne(
            publicationSeriesAssessmentClassificationId)).thenReturn(
            existingPublicationSeriesAssessmentClassification);
        when(publicationSeriesService.findOne(1)).thenReturn(new Journal() {{
            setId(1);
        }});
        when(assessmentClassificationService.findOne(1)).thenReturn(new AssessmentClassification());

        // when
        publicationSeriesAssessmentClassificationService.updatePublicationSeriesAssessmentClassification(
            publicationSeriesAssessmentClassificationId,
            publicationSeriesAssessmentClassificationDTO);

        // then
        verify(publicationSeriesAssessmentClassificationJPAService).findOne(
            publicationSeriesAssessmentClassificationId);
        verify(publicationSeriesAssessmentClassificationJPAService).save(
            existingPublicationSeriesAssessmentClassification);
        verify(journalService).reindexJournalVolatileInformation(anyInt());
    }

    @Test
    void shouldScheduleClassification() {
        // Given
        var timeToRun = LocalDateTime.now().plusDays(1);
        var commissionId = 1;
        var userId = 42;
        var classificationYears = List.of(2023, 2024);

        var commission = new Commission();
        commission.setFormalDescriptionOfRule("CustomRuleEngine");

        when(commissionService.findOne(commissionId)).thenReturn(commission);

        // When
        publicationSeriesAssessmentClassificationService.scheduleClassification(timeToRun,
            commissionId, userId, classificationYears, new ArrayList<>());

        // Then
        verify(taskManagerService, times(1)).scheduleTask(
            matches("Publication_Series_Classification-CustomRuleEngine-.*"),
            eq(timeToRun),
            any(Runnable.class),
            eq(userId),
            eq(RecurrenceType.ONCE));
    }
}
