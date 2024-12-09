package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.dto.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.PublicationSeriesAssessmentClassification;
import rs.teslaris.core.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.impl.PublicationSeriesAssessmentClassificationServiceImpl;
import rs.teslaris.core.assessment.service.impl.cruddelegate.PublicationSeriesAssessmentClassificationJPAServiceImpl;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.model.document.Journal;
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

        var publicationSeriesAssessmentClassification2 =
            new PublicationSeriesAssessmentClassification();
        publicationSeriesAssessmentClassification2.setAssessmentClassification(
            assessmentClassification);

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
        var publicationSeriesAssessmentClassificationDTO =
            new PublicationSeriesAssessmentClassificationDTO();
        publicationSeriesAssessmentClassificationDTO.setPublicationSeriesId(1);
        publicationSeriesAssessmentClassificationDTO.setAssessmentClassificationId(1);

        var newPublicationSeriesAssessmentClassification =
            new PublicationSeriesAssessmentClassification();
        newPublicationSeriesAssessmentClassification.setAssessmentClassification(
            new AssessmentClassification());

        when(publicationSeriesService.findOne(1)).thenReturn(new Journal());
        when(publicationSeriesAssessmentClassificationJPAService.save(
            any(PublicationSeriesAssessmentClassification.class)))
            .thenReturn(newPublicationSeriesAssessmentClassification);
        when(assessmentClassificationService.findOne(1)).thenReturn(new AssessmentClassification());

        var result =
            publicationSeriesAssessmentClassificationService.createPublicationSeriesAssessmentClassification(
                publicationSeriesAssessmentClassificationDTO);

        assertNotNull(result);
        verify(publicationSeriesAssessmentClassificationJPAService).save(
            any(PublicationSeriesAssessmentClassification.class));
    }

    @Test
    void shouldUpdatePublicationSeriesAssessmentClassification() {
        var publicationSeriesAssessmentClassificationId = 1;
        var publicationSeriesAssessmentClassificationDTO =
            new PublicationSeriesAssessmentClassificationDTO();
        publicationSeriesAssessmentClassificationDTO.setPublicationSeriesId(1);
        publicationSeriesAssessmentClassificationDTO.setAssessmentClassificationId(1);

        var existingPublicationSeriesAssessmentClassification =
            new PublicationSeriesAssessmentClassification();
        existingPublicationSeriesAssessmentClassification.setAssessmentClassification(
            new AssessmentClassification());

        when(publicationSeriesAssessmentClassificationJPAService.findOne(
            publicationSeriesAssessmentClassificationId)).thenReturn(
            existingPublicationSeriesAssessmentClassification);
        when(publicationSeriesService.findOne(1)).thenReturn(new Journal());
        when(assessmentClassificationService.findOne(1)).thenReturn(new AssessmentClassification());

        publicationSeriesAssessmentClassificationService.updatePublicationSeriesAssessmentClassification(
            publicationSeriesAssessmentClassificationId,
            publicationSeriesAssessmentClassificationDTO);

        verify(publicationSeriesAssessmentClassificationJPAService).findOne(
            publicationSeriesAssessmentClassificationId);
        verify(publicationSeriesAssessmentClassificationJPAService).save(
            existingPublicationSeriesAssessmentClassification);
    }
}
