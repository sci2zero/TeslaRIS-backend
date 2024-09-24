package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.assessment.AssessmentClassificationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.assessment.AssessmentClassification;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.assessment.AssessmentClassificationRepository;
import rs.teslaris.core.service.impl.assessment.AssessmentClassificationServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;

@SpringBootTest
public class AssessmentClassificationServiceTest {

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private AssessmentClassificationRepository assessmentClassificationRepository;

    @InjectMocks
    private AssessmentClassificationServiceImpl assessmentClassificationService;

    
    @Test
    void shouldReadAssessmentClassification() {
        var assessmentClassificationId = 1;
        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setTitle(
            Set.of(new MultiLingualContent(new LanguageTag(), "Content", 1)));
        when(assessmentClassificationRepository.findById(assessmentClassificationId))
            .thenReturn(Optional.of(assessmentClassification));

        var dto = new AssessmentClassificationDTO(null, null,
            List.of(new MultilingualContentDTO(null, null, "Content", 1)));

        var result = assessmentClassificationService.readAssessmentClassification(
            assessmentClassificationId);

        assertEquals(dto.code(), result.code());
        verify(assessmentClassificationRepository).findById(assessmentClassificationId);
    }

    @Test
    void shouldCreateAssessmentClassification() {
        var assessmentClassificationDTO = new AssessmentClassificationDTO("rule", "M22",
            List.of(new MultilingualContentDTO()));
        var newAssessmentClassification = new AssessmentClassification();

        when(assessmentClassificationRepository.save(any(AssessmentClassification.class)))
            .thenReturn(newAssessmentClassification);

        var result = assessmentClassificationService.createAssessmentCLassification(
            assessmentClassificationDTO);

        assertNotNull(result);
        verify(assessmentClassificationRepository).save(any(AssessmentClassification.class));
    }

    @Test
    void shouldUpdateAssessmentClassification() {
        var assessmentClassificationId = 1;
        var assessmentClassificationDTO = new AssessmentClassificationDTO("rule", "M21",
            List.of(new MultilingualContentDTO()));
        var existingAssessmentClassification = new AssessmentClassification();

        when(assessmentClassificationRepository.findById(assessmentClassificationId))
            .thenReturn(Optional.of(existingAssessmentClassification));

        assessmentClassificationService.updateAssessmentClassification(assessmentClassificationId,
            assessmentClassificationDTO);

        verify(assessmentClassificationRepository).findById(assessmentClassificationId);
        verify(assessmentClassificationRepository).save(existingAssessmentClassification);
    }
}
