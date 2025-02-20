package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import rs.teslaris.core.assessment.dto.AssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.ApplicableEntityType;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.repository.AssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.impl.AssessmentClassificationServiceImpl;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.AssessmentClassificationReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class AssessmentClassificationServiceTest {

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private AssessmentClassificationRepository assessmentClassificationRepository;

    @InjectMocks
    private AssessmentClassificationServiceImpl assessmentClassificationService;


    @Test
    void shouldReadAllAssessmentClassifications() {
        var assessmentClassification1 = new AssessmentClassification();
        assessmentClassification1.setCode("code1");
        assessmentClassification1.setFormalDescriptionOfRule("rule1");

        var assessmentClassification2 = new AssessmentClassification();
        assessmentClassification2.setCode("code2");
        assessmentClassification2.setFormalDescriptionOfRule("rule2");

        when(assessmentClassificationRepository.findAll(any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(assessmentClassification1, assessmentClassification2)));

        var response =
            assessmentClassificationService.readAllAssessmentClassifications(PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(2, response.getSize());
    }

    @Test
    void shouldReadAssessmentClassification() {
        var assessmentClassificationId = 1;
        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setTitle(
            Set.of(new MultiLingualContent(new LanguageTag(), "Content", 1)));
        when(assessmentClassificationRepository.findById(assessmentClassificationId))
            .thenReturn(Optional.of(assessmentClassification));

        var dto = new AssessmentClassificationDTO(null, null, null,
            List.of(new MultilingualContentDTO(null, null, "Content", 1)),
            List.of(ApplicableEntityType.EVENT));

        var result = assessmentClassificationService.readAssessmentClassification(
            assessmentClassificationId);

        assertEquals(dto.code(), result.code());
        verify(assessmentClassificationRepository).findById(assessmentClassificationId);
    }

    @Test
    void shouldCreateAssessmentClassification() {
        var assessmentClassificationDTO = new AssessmentClassificationDTO(null, "rule", "M22",
            List.of(new MultilingualContentDTO()), List.of(ApplicableEntityType.EVENT));
        var newAssessmentClassification = new AssessmentClassification();

        when(assessmentClassificationRepository.save(any(AssessmentClassification.class)))
            .thenReturn(newAssessmentClassification);

        var result = assessmentClassificationService.createAssessmentClassification(
            assessmentClassificationDTO);

        assertNotNull(result);
        verify(assessmentClassificationRepository).save(any(AssessmentClassification.class));
    }

    @Test
    void shouldUpdateAssessmentClassification() {
        var assessmentClassificationId = 1;
        var assessmentClassificationDTO = new AssessmentClassificationDTO(null, "rule", "M21",
            List.of(new MultilingualContentDTO()), List.of(ApplicableEntityType.EVENT));
        var existingAssessmentClassification = new AssessmentClassification();

        when(assessmentClassificationRepository.findById(assessmentClassificationId))
            .thenReturn(Optional.of(existingAssessmentClassification));

        assessmentClassificationService.updateAssessmentClassification(assessmentClassificationId,
            assessmentClassificationDTO);

        verify(assessmentClassificationRepository).findById(assessmentClassificationId);
        verify(assessmentClassificationRepository).save(existingAssessmentClassification);
    }

    @Test
    void shouldDeleteAssessmentClassification() {
        // Given
        var assessmentClassificationId = 1;

        when(assessmentClassificationRepository.isInUse(assessmentClassificationId))
            .thenReturn(false);

        when(assessmentClassificationRepository.findById(assessmentClassificationId)).thenReturn(
            Optional.of(new AssessmentClassification()));

        // When
        assessmentClassificationService.deleteAssessmentClassification(assessmentClassificationId);

        // Then
        verify(assessmentClassificationRepository).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDeletingAssessmentClassificationInUse() {
        // Given
        var assessmentClassificationId = 1;

        when(assessmentClassificationRepository.isInUse(assessmentClassificationId))
            .thenReturn(true);

        // When
        assertThrows(AssessmentClassificationReferenceConstraintViolationException.class, () ->
            assessmentClassificationService.deleteAssessmentClassification(
                assessmentClassificationId));

        // Then (AssessmentClassificationReferenceConstraintViolationException should be thrown)
    }

    @Test
    void shouldReadAssessmentClassificationByCode() {
        // Given
        var code = "M21APlus";

        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setCode(code);

        when(assessmentClassificationRepository.findByCode(code))
            .thenReturn(Optional.of(assessmentClassification));

        // When
        var result = assessmentClassificationService.readAssessmentClassificationByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(assessmentClassificationRepository, times(1)).findByCode(code);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenCodeDoesNotExist() {
        // Given
        var code = "NonExistentCode";

        when(assessmentClassificationRepository.findByCode(code))
            .thenReturn(Optional.empty());

        // When & Then
        var exception = assertThrows(NotFoundException.class,
            () -> assessmentClassificationService.readAssessmentClassificationByCode(code));

        assertEquals("Assessment Classification with given code does not exist - NonExistentCode.",
            exception.getMessage());
        verify(assessmentClassificationRepository, times(1)).findByCode(code);
    }

    @Test
    public void shouldGetClassificationsApplicableToEntity() {
        // given
        var classification1 = new AssessmentClassification();
        classification1.setId(1);
        var classification2 = new AssessmentClassification();
        classification2.setId(2);

        var applicableEntityTypes =
            new ArrayList<>(List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON));
        var mockClassifications = List.of(classification2, classification1);

        // Mock repository call
        when(assessmentClassificationRepository.getAssessmentClassificationsApplicableToEntity(
            applicableEntityTypes))
            .thenReturn(mockClassifications);

        // when
        var result = assessmentClassificationService.getAssessmentClassificationsApplicableToEntity(
            applicableEntityTypes);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(assessmentClassificationRepository).getAssessmentClassificationsApplicableToEntity(
            applicableEntityTypes);
    }

    @Test
    public void shouldReturnEmptyListWhenNoCLassificationsFound() {
        // given
        var applicableEntityTypes = new ArrayList<>(List.of(ApplicableEntityType.DOCUMENT));

        when(assessmentClassificationRepository.getAssessmentClassificationsApplicableToEntity(
            applicableEntityTypes))
            .thenReturn(List.of());

        // when
        var result = assessmentClassificationService.getAssessmentClassificationsApplicableToEntity(
            applicableEntityTypes);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(assessmentClassificationRepository).getAssessmentClassificationsApplicableToEntity(
            applicableEntityTypes);
    }
}
