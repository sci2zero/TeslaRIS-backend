package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import rs.teslaris.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.assessment.model.AssessmentMeasure;
import rs.teslaris.assessment.model.AssessmentRulebook;
import rs.teslaris.assessment.repository.AssessmentMeasureRepository;
import rs.teslaris.assessment.service.impl.AssessmentMeasureServiceImpl;
import rs.teslaris.assessment.service.interfaces.AssessmentRulebookService;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;

@SpringBootTest
public class AssessmentMeasureServiceTest {

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private AssessmentMeasureRepository assessmentMeasureRepository;

    @Mock
    private AssessmentRulebookService assessmentRulebookService;

    @InjectMocks
    private AssessmentMeasureServiceImpl assessmentMeasureService;


    @Test
    void shouldReadAllAssessmentMeasures() {
        // given
        var assessmentMeasure1 = new AssessmentMeasure();
        assessmentMeasure1.setCode("code1");
        assessmentMeasure1.setPointRule("serbianPointsRulebook2025");
        assessmentMeasure1.setScalingRule("serbianScalingRulebook2025");

        var assessmentMeasure2 = new AssessmentMeasure();
        assessmentMeasure2.setCode("code2");
        assessmentMeasure2.setPointRule("serbianPointsRulebook2025");
        assessmentMeasure2.setScalingRule("serbianScalingRulebook2025");

        when(assessmentMeasureRepository.searchAssessmentMeasures(any(Pageable.class),
            anyString())).thenReturn(
            new PageImpl<>(List.of(assessmentMeasure1, assessmentMeasure2)));

        // when
        var response =
            assessmentMeasureService.searchAssessmentMeasures(PageRequest.of(0, 10), "code");

        // then
        assertNotNull(response);
        assertEquals(2, response.getSize());
    }

    @Test
    void shouldReadAssessmentMeasure() {
        // given
        var assessmentMeasureId = 1;
        var assessmentMeasure = new AssessmentMeasure();
        assessmentMeasure.setTitle(
            Set.of(new MultiLingualContent(new LanguageTag(), "Content", 1)));
        assessmentMeasure.setRulebook(new AssessmentRulebook());
        when(assessmentMeasureRepository.findById(assessmentMeasureId))
            .thenReturn(Optional.of(assessmentMeasure));
        when(assessmentRulebookService.findOne(1)).thenReturn(new AssessmentRulebook());

        var dto = new AssessmentMeasureDTO(null, null, null, null,
            List.of(new MultilingualContentDTO(null, null, "Content", 1)), 1);

        // when
        var result = assessmentMeasureService.readAssessmentMeasureById(
            assessmentMeasureId);

        // then
        assertEquals(dto.code(), result.code());
        verify(assessmentMeasureRepository).findById(assessmentMeasureId);
    }

    @Test
    void shouldCreateAssessmentMeasure() {
        // given
        var assessmentMeasureDTO =
            new AssessmentMeasureDTO(null, "rule", "serbianPointsRulebook2025",
                "serbianScalingRulebook2025",
                List.of(new MultilingualContentDTO()), 1);
        var newAssessmentMeasure = new AssessmentMeasure();
        newAssessmentMeasure.setRulebook(new AssessmentRulebook());

        when(assessmentMeasureRepository.save(any(AssessmentMeasure.class)))
            .thenReturn(newAssessmentMeasure);
        when(assessmentRulebookService.findOne(1)).thenReturn(new AssessmentRulebook());

        // when
        var result = assessmentMeasureService.createAssessmentMeasure(
            assessmentMeasureDTO);

        // then
        assertNotNull(result);
        verify(assessmentMeasureRepository).save(any(AssessmentMeasure.class));
    }

    @Test
    void shouldUpdateAssessmentMeasure() {
        // given
        var assessmentMeasureId = 1;
        var assessmentMeasureDTO =
            new AssessmentMeasureDTO(null, "rule", "serbianPointsRulebook2025",
                "serbianScalingRulebook2025",
                List.of(new MultilingualContentDTO()), 1);
        var existingAssessmentMeasure = new AssessmentMeasure();

        when(assessmentMeasureRepository.findById(assessmentMeasureId))
            .thenReturn(Optional.of(existingAssessmentMeasure));
        when(assessmentRulebookService.findOne(1)).thenReturn(new AssessmentRulebook());

        // when
        assessmentMeasureService.updateAssessmentMeasure(assessmentMeasureId,
            assessmentMeasureDTO);

        // then
        verify(assessmentMeasureRepository).findById(assessmentMeasureId);
        verify(assessmentMeasureRepository).save(existingAssessmentMeasure);
    }

    @Test
    void shouldDeleteAssessmentMeasure() {
        // Given
        var assessmentMeasureId = 1;
        var measure = new AssessmentMeasure();
        var rulebook = new AssessmentRulebook();
        rulebook.getAssessmentMeasures().add(measure);
        measure.setRulebook(rulebook);

        when(assessmentMeasureRepository.findById(assessmentMeasureId)).thenReturn(
            Optional.of(measure));

        // When
        assessmentMeasureService.deleteAssessmentMeasure(assessmentMeasureId);

        // Then
        verify(assessmentMeasureRepository).save(any());
    }

    @Test
    void shouldListAllPointRules() {
        // When
        var result = assessmentMeasureService.listAllPointRules();

        // Then
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldListAllScalingRules() {
        // When
        var result = assessmentMeasureService.listAllScalingRules();

        // Then
        assertFalse(result.isEmpty());
    }
}
