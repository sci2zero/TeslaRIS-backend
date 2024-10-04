package rs.teslaris.core.unit.assessment;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.model.DocumentAssessmentClassification;
import rs.teslaris.core.assessment.repository.EntityAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.impl.EntityAssessmentClassificationServiceImpl;

@SpringBootTest
public class EntityAssessmentClassificationServiceTest {

    @Mock
    private EntityAssessmentClassificationRepository entityAssessmentClassificationRepository;

    @InjectMocks
    private EntityAssessmentClassificationServiceImpl entityAssessmentClassificationService;


    @Test
    public void shouldDeleteEntityAssessmentClassification() {
        // Given
        var entityAssessmentClassificationId = 1;
        var entityAssessmentClassification = new DocumentAssessmentClassification();

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
