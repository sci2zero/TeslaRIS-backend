package rs.teslaris.core.unit.assessment;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.assessment.model.classification.DocumentAssessmentClassification;
import rs.teslaris.assessment.repository.classification.EntityAssessmentClassificationRepository;
import rs.teslaris.assessment.service.impl.classification.EntityAssessmentClassificationServiceImpl;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;

@SpringBootTest
public class EntityAssessmentClassificationServiceTest {

    @Mock
    private EntityAssessmentClassificationRepository entityAssessmentClassificationRepository;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @InjectMocks
    private EntityAssessmentClassificationServiceImpl entityAssessmentClassificationService;


    @Test
    public void shouldDeleteEntityAssessmentClassification() {
        // Given
        var entityAssessmentClassificationId = 1;
        var entityAssessmentClassification = new DocumentAssessmentClassification();
        entityAssessmentClassification.setDocument(new Monograph());

        when(entityAssessmentClassificationRepository.findById(
            entityAssessmentClassificationId)).thenReturn(
            Optional.of(entityAssessmentClassification));

        // When
        entityAssessmentClassificationService.deleteEntityAssessmentClassification(
            entityAssessmentClassificationId);

        // Then
        verify(entityAssessmentClassificationRepository, times(1)).delete(
            entityAssessmentClassification);
    }
}
