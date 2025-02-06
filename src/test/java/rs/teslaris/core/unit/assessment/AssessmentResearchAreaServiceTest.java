package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.model.AssessmentResearchArea;
import rs.teslaris.core.assessment.model.Commission;
import rs.teslaris.core.assessment.repository.AssessmentResearchAreaRepository;
import rs.teslaris.core.assessment.service.impl.AssessmentResearchAreaServiceImpl;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class AssessmentResearchAreaServiceTest {

    private final Integer personId = 1;

    private final String researchAreaCode = "TECHNICAL";

    private final Integer commissionId = 10;

    @Mock
    private AssessmentResearchAreaRepository assessmentResearchAreaRepository;

    @Mock
    private PersonService personService;

    @Mock
    private CommissionService commissionService;

    @InjectMocks
    private AssessmentResearchAreaServiceImpl assessmentResearchAreaService;


    @Test
    void shouldReadPersonAssessmentResearchArea() {
        // Given
        var researchArea = new AssessmentResearchArea();
        researchArea.setResearchAreaCode(researchAreaCode);
        when(assessmentResearchAreaRepository.findForPersonId(personId)).thenReturn(
            Optional.of(researchArea));

        // When
        var result = assessmentResearchAreaService.readPersonAssessmentResearchArea(personId);

        // Then
        assertNotNull(result);
        assertEquals(researchAreaCode, result.getCode());
    }

    @Test
    void shouldRetrnNullWhenReadingNonExistentPersonAssessmentResearchArea() {
        // Given
        when(assessmentResearchAreaRepository.findForPersonId(personId)).thenReturn(
            Optional.empty());

        // When
        var result = assessmentResearchAreaService.readPersonAssessmentResearchArea(personId);

        // Then
        assertNull(result);
    }

    @Test
    void shouldSetPersonAssessmentResearchAreaWhenExists() {
        // Given
        var researchArea = new AssessmentResearchArea();
        researchArea.setResearchAreaCode("OLD_CODE");
        when(assessmentResearchAreaRepository.findForPersonId(personId)).thenReturn(
            Optional.of(researchArea));

        // When
        assessmentResearchAreaService.setPersonAssessmentResearchArea(personId, researchAreaCode);

        // Then
        assertEquals(researchAreaCode, researchArea.getResearchAreaCode());
        verify(assessmentResearchAreaRepository).save(researchArea);
    }

    @Test
    void shouldCreateNewPersonAssessmentResearchAreaIfNotExists() {
        // Given
        when(assessmentResearchAreaRepository.findForPersonId(personId)).thenReturn(
            Optional.empty());
        when(personService.findOne(personId)).thenReturn(new Person());

        // When
        assessmentResearchAreaService.setPersonAssessmentResearchArea(personId, researchAreaCode);

        // Then
        verify(assessmentResearchAreaRepository).save(any(AssessmentResearchArea.class));
    }

    @Test
    void shouldDeletePersonAssessmentResearchAreaIfExists() {
        // Given
        var researchArea = new AssessmentResearchArea();
        when(assessmentResearchAreaRepository.findForPersonId(personId)).thenReturn(
            Optional.of(researchArea));

        // When
        assessmentResearchAreaService.deletePersonAssessmentResearchArea(personId);

        // Then
        verify(assessmentResearchAreaRepository).delete(researchArea);
    }

    @Test
    void shouldNotDeletePersonAssessmentResearchAreaIfNotExists() {
        // Given
        when(assessmentResearchAreaRepository.findForPersonId(personId)).thenReturn(
            Optional.empty());

        // When
        assessmentResearchAreaService.deletePersonAssessmentResearchArea(personId);

        // Then
        verify(assessmentResearchAreaRepository, never()).delete(any());
    }

    @Test
    void shouldCreateNewPersonAssessmentResearchAreaForCommissionIfNotExists() {
        // Given
        when(
            assessmentResearchAreaRepository.findForPersonIdAndCommissionId(personId, commissionId))
            .thenReturn(Optional.empty());
        when(personService.findOne(personId)).thenReturn(new Person());
        when(commissionService.findOne(commissionId)).thenReturn(new Commission());

        // When
        assessmentResearchAreaService.setPersonAssessmentResearchAreaForCommission(personId,
            researchAreaCode, commissionId);

        // Then
        verify(assessmentResearchAreaRepository).save(any(AssessmentResearchArea.class));
    }

    @Test
    void shouldUpdateExistingPersonAssessmentResearchArea() {
        // Given
        AssessmentResearchArea existingResearchArea = new AssessmentResearchArea();
        existingResearchArea.setResearchAreaCode("OLD_RA");
        when(
            assessmentResearchAreaRepository.findForPersonIdAndCommissionId(personId, commissionId))
            .thenReturn(Optional.of(existingResearchArea));
        when(commissionService.findOne(commissionId)).thenReturn(new Commission());

        // When
        assessmentResearchAreaService.setPersonAssessmentResearchAreaForCommission(personId,
            researchAreaCode, commissionId);

        // Then
        assertEquals(researchAreaCode, existingResearchArea.getResearchAreaCode());
        verify(assessmentResearchAreaRepository).save(existingResearchArea);
    }

    @Test
    void shouldThrowNotFoundExceptionIfResearchAreaCodeDoesNotExist() {
        // When & Then
        assertThrows(NotFoundException.class,
            () -> assessmentResearchAreaService.setPersonAssessmentResearchAreaForCommission(
                personId, "NON_EXISTANT", commissionId));
        verify(assessmentResearchAreaRepository, never()).save(any());
    }

    @Test
    void shouldRemovePersonAssessmentResearchAreaForCommission() {
        // Given
        AssessmentResearchArea existingResearchArea = new AssessmentResearchArea();
        existingResearchArea.setResearchAreaCode(researchAreaCode);
        when(
            assessmentResearchAreaRepository.findForPersonIdAndCommissionId(personId, commissionId))
            .thenReturn(Optional.of(existingResearchArea));

        // When
        assessmentResearchAreaService.removePersonAssessmentResearchAreaForCommission(personId,
            commissionId);

        // Then
        assertNull(existingResearchArea.getResearchAreaCode());
        verify(assessmentResearchAreaRepository).save(existingResearchArea);
    }

    @Test
    void shouldDoNothingIfRemovingNonExistentResearchArea() {
        // Given
        when(
            assessmentResearchAreaRepository.findForPersonIdAndCommissionId(personId, commissionId))
            .thenReturn(Optional.empty());

        // When
        assessmentResearchAreaService.removePersonAssessmentResearchAreaForCommission(personId,
            commissionId);

        // Then
        verify(assessmentResearchAreaRepository, never()).save(any());
    }
}
