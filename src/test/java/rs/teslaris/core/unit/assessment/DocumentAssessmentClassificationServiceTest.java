package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.DocumentAssessmentClassification;
import rs.teslaris.core.assessment.repository.DocumentAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.impl.DocumentAssessmentClassificationServiceImpl;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;

@SpringBootTest
public class DocumentAssessmentClassificationServiceTest {

    @Mock
    private DocumentAssessmentClassificationRepository
        documentAssessmentClassificationRepository;

    @Mock
    private TaskManagerService taskManagerService;

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

    @ParameterizedTest
    @EnumSource(value = DocumentPublicationType.class, names = {"JOURNAL_PUBLICATION",
        "PROCEEDINGS_PUBLICATION"})
    void shouldScheduleJournalPublicationClassificationTask(
        DocumentPublicationType documentPublicationType) {
        // Given
        var timeToRun = LocalDateTime.of(2025, 1, 28, 10, 0);
        var userId = 123;
        var fromDate = LocalDate.of(2025, 1, 1);

        // When
        documentAssessmentClassificationService.schedulePublicationClassification(timeToRun,
            userId, fromDate, documentPublicationType, null, new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>());

        // Then
        verify(taskManagerService, times(1)).scheduleTask(
            argThat(
                taskName -> taskName.startsWith(
                    documentPublicationType.name() + "_Assessment-From-" + fromDate)),
            eq(timeToRun),
            any(Runnable.class),
            eq(userId)
        );
    }
}