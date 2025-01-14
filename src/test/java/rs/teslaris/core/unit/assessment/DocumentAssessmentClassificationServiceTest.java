package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.DocumentAssessmentClassification;
import rs.teslaris.core.assessment.repository.DocumentAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.impl.DocumentAssessmentClassificationServiceImpl;

@SpringBootTest
public class DocumentAssessmentClassificationServiceTest {

    @Mock
    private DocumentAssessmentClassificationRepository
        documentAssessmentClassificationRepository;

    @InjectMocks
    private DocumentAssessmentClassificationServiceImpl documentAssessmentClassificationService;

    @Test
    void shouldReadAllDocumentAssessmentClassificationsForDocument() {
        // Given
        var documentId = 1;

        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setCode("code");

        var documentAssessmentClassification1 = new DocumentAssessmentClassification();
        documentAssessmentClassification1.setAssessmentClassification(assessmentClassification);
        documentAssessmentClassification1.setClassificationYear(2025);

        var documentAssessmentClassification2 = new DocumentAssessmentClassification();
        documentAssessmentClassification2.setAssessmentClassification(assessmentClassification);
        documentAssessmentClassification2.setClassificationYear(2025);

        when(
            documentAssessmentClassificationRepository.findAssessmentClassificationsForDocument(
                documentId)).thenReturn(
            List.of(documentAssessmentClassification1, documentAssessmentClassification2));

        // When
        var response =
            documentAssessmentClassificationService.getAssessmentClassificationsForDocument(
                documentId);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
    }
}