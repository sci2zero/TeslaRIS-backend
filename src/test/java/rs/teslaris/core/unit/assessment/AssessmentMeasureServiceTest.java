package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.core.assessment.model.AssessmentMeasure;
import rs.teslaris.core.assessment.repository.AssessmentMeasureRepository;
import rs.teslaris.core.assessment.service.impl.AssessmentMeasureServiceImpl;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.AssessmentMeasureReferenceConstraintViolationException;

@SpringBootTest
public class AssessmentMeasureServiceTest {

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private AssessmentMeasureRepository assessmentMeasureRepository;

    @InjectMocks
    private AssessmentMeasureServiceImpl assessmentMeasureService;


    @Test
    void shouldReadAllAssessmentMeasures() {
        var assessmentMeasure1 = new AssessmentMeasure();
        assessmentMeasure1.setCode("code1");
        assessmentMeasure1.setFormalDescriptionOfRule("rule1");

        var assessmentMeasure2 = new AssessmentMeasure();
        assessmentMeasure2.setCode("code2");
        assessmentMeasure2.setFormalDescriptionOfRule("rule2");

        when(assessmentMeasureRepository.findAll(any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(assessmentMeasure1, assessmentMeasure2)));

        var response =
            assessmentMeasureService.readAllAssessmentMeasures(PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(2, response.getSize());
    }

    @Test
    void shouldReadAssessmentMeasure() {
        var assessmentMeasureId = 1;
        var assessmentMeasure = new AssessmentMeasure();
        assessmentMeasure.setTitle(
            Set.of(new MultiLingualContent(new LanguageTag(), "Content", 1)));
        when(assessmentMeasureRepository.findById(assessmentMeasureId))
            .thenReturn(Optional.of(assessmentMeasure));

        var dto = new AssessmentMeasureDTO(null, null, null, null,
            List.of(new MultilingualContentDTO(null, null, "Content", 1)));

        var result = assessmentMeasureService.readAssessmentMeasureById(
            assessmentMeasureId);

        assertEquals(dto.code(), result.code());
        verify(assessmentMeasureRepository).findById(assessmentMeasureId);
    }

    @Test
    void shouldCreateAssessmentMeasure() {
        var assessmentMeasureDTO = new AssessmentMeasureDTO(null, "rule", "M22", 1d,
            List.of(new MultilingualContentDTO()));
        var newAssessmentMeasure = new AssessmentMeasure();

        when(assessmentMeasureRepository.save(any(AssessmentMeasure.class)))
            .thenReturn(newAssessmentMeasure);

        var result = assessmentMeasureService.createAssessmentMeasure(
            assessmentMeasureDTO);

        assertNotNull(result);
        verify(assessmentMeasureRepository).save(any(AssessmentMeasure.class));
    }

    @Test
    void shouldUpdateAssessmentMeasure() {
        var assessmentMeasureId = 1;
        var assessmentMeasureDTO = new AssessmentMeasureDTO(null, "rule", "M21", 2d,
            List.of(new MultilingualContentDTO()));
        var existingAssessmentMeasure = new AssessmentMeasure();

        when(assessmentMeasureRepository.findById(assessmentMeasureId))
            .thenReturn(Optional.of(existingAssessmentMeasure));

        assessmentMeasureService.updateAssessmentMeasure(assessmentMeasureId,
            assessmentMeasureDTO);

        verify(assessmentMeasureRepository).findById(assessmentMeasureId);
        verify(assessmentMeasureRepository).save(existingAssessmentMeasure);
    }

    @Test
    void shouldDeleteAssessmentMeasure() {
        // Given
        var assessmentMeasureId = 1;

        when(assessmentMeasureRepository.isInUse(assessmentMeasureId))
            .thenReturn(false);

        when(assessmentMeasureRepository.findById(assessmentMeasureId)).thenReturn(
            Optional.of(new AssessmentMeasure()));

        // When
        assessmentMeasureService.deleteAssessmentMeasure(assessmentMeasureId);

        // Then
        verify(assessmentMeasureRepository).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDeletingAssessmentMeasureInUse() {
        // Given
        var assessmentMeasureId = 1;

        when(assessmentMeasureRepository.isInUse(assessmentMeasureId))
            .thenReturn(true);

        // When
        assertThrows(AssessmentMeasureReferenceConstraintViolationException.class, () ->
            assessmentMeasureService.deleteAssessmentMeasure(
                assessmentMeasureId));

        // Then (AssessmentMeasureReferenceConstraintViolationException should be thrown)
    }
}
