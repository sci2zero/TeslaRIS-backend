package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import rs.teslaris.assessment.dto.classification.PrizeAssessmentClassificationDTO;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.assessment.model.classification.PrizeAssessmentClassification;
import rs.teslaris.assessment.repository.classification.EntityAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.PrizeAssessmentClassificationRepository;
import rs.teslaris.assessment.service.impl.classification.PrizeAssessmentClassificationServiceImpl;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.core.applicationevent.ResearcherPointsReindexingEvent;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.repository.person.PrizeRepository;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class PrizeAssessmentClassificationServiceTest {

    @Mock
    private AssessmentClassificationService assessmentClassificationService;

    @Mock
    private PrizeAssessmentClassificationRepository prizeAssessmentClassificationRepository;

    @Mock
    private PrizeRepository prizeRepository;

    @Mock
    private PrizeService prizeService;

    @Mock
    private CommissionService commissionService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private EntityAssessmentClassificationRepository entityAssessmentClassificationRepository;

    @InjectMocks
    private PrizeAssessmentClassificationServiceImpl prizeAssessmentClassificationService;


    @Test
    void shouldReadAllPrizeAssessmentClassificationsForPrize() {
        // Given
        var prizeId = 1;

        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setCode("code");

        var prizeAssessmentClassification1 = new PrizeAssessmentClassification();
        prizeAssessmentClassification1.setAssessmentClassification(assessmentClassification);
        prizeAssessmentClassification1.setClassificationYear(2025);

        var prizeAssessmentClassification2 = new PrizeAssessmentClassification();
        prizeAssessmentClassification2.setAssessmentClassification(assessmentClassification);
        prizeAssessmentClassification2.setClassificationYear(2024);

        when(prizeAssessmentClassificationRepository.findAssessmentClassificationsForPrize(prizeId))
            .thenReturn(List.of(prizeAssessmentClassification1, prizeAssessmentClassification2));

        // When
        var response =
            prizeAssessmentClassificationService.getAssessmentClassificationsForPrize(prizeId);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals(2025, response.get(0).year());
        assertEquals(2024, response.get(1).year());
    }

    @Test
    void shouldReturnEmptyListWhenNoClassificationsFoundForPrize() {
        // Given
        var prizeId = 1;
        when(prizeAssessmentClassificationRepository.findAssessmentClassificationsForPrize(prizeId))
            .thenReturn(Collections.emptyList());

        // When
        var response =
            prizeAssessmentClassificationService.getAssessmentClassificationsForPrize(prizeId);

        // Then
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void shouldSortClassificationsByYearDescending() {
        // Given
        var prizeId = 1;

        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setCode("code");

        var prizeClassification2023 = new PrizeAssessmentClassification();
        prizeClassification2023.setAssessmentClassification(assessmentClassification);
        prizeClassification2023.setClassificationYear(2023);

        var prizeClassification2025 = new PrizeAssessmentClassification();
        prizeClassification2025.setAssessmentClassification(assessmentClassification);
        prizeClassification2025.setClassificationYear(2025);

        var prizeClassification2024 = new PrizeAssessmentClassification();
        prizeClassification2024.setAssessmentClassification(assessmentClassification);
        prizeClassification2024.setClassificationYear(2024);

        when(prizeAssessmentClassificationRepository.findAssessmentClassificationsForPrize(prizeId))
            .thenReturn(
                List.of(prizeClassification2023, prizeClassification2025, prizeClassification2024));

        // When
        var response =
            prizeAssessmentClassificationService.getAssessmentClassificationsForPrize(prizeId);

        // Then
        assertEquals(3, response.size());
        assertEquals(2025, response.get(0).year());
        assertEquals(2024, response.get(1).year());
        assertEquals(2023, response.get(2).year());
    }

    @Test
    void shouldReturnSingleClassificationForPrize() {
        // Given
        var prizeId = 1;

        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setCode("TEST_CODE");
        assessmentClassification.setTitle(
            Set.of(new MultiLingualContent(new LanguageTag(), "Content", 1)));

        var prizeAssessmentClassification = new PrizeAssessmentClassification();
        prizeAssessmentClassification.setAssessmentClassification(assessmentClassification);
        prizeAssessmentClassification.setClassificationYear(2024);

        when(prizeAssessmentClassificationRepository.findAssessmentClassificationsForPrize(prizeId))
            .thenReturn(List.of(prizeAssessmentClassification));

        // When
        var response =
            prizeAssessmentClassificationService.getAssessmentClassificationsForPrize(prizeId);

        // Then
        assertEquals(1, response.size());
        assertEquals(2024, response.getFirst().year());
    }

    @Test
    void shouldHandleNullValuesInClassificationData() {
        // Given
        var prizeId = 1;

        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setCode("code");

        var prizeAssessmentClassification = new PrizeAssessmentClassification();
        prizeAssessmentClassification.setAssessmentClassification(assessmentClassification);
        prizeAssessmentClassification.setClassificationYear(null);

        when(prizeAssessmentClassificationRepository.findAssessmentClassificationsForPrize(prizeId))
            .thenReturn(List.of(prizeAssessmentClassification));

        // When
        var response =
            prizeAssessmentClassificationService.getAssessmentClassificationsForPrize(prizeId);

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertNull(response.get(0).year());
    }

    @Test
    void shouldCreatePrizeAssessmentClassification() {
        // Given
        var prizeId = 1;
        var commissionId = 10;
        var classificationYear = 2024;

        var prize = new Prize();
        prize.setId(prizeId);
        prize.setDate(LocalDate.of(classificationYear, 1, 1));

        var person = new Person();
        person.setId(100);
        prize.setPerson(person);

        var commission = new Commission();
        commission.setId(commissionId);

        var prizeDTO = new PrizeAssessmentClassificationDTO();
        prizeDTO.setPrizeId(prizeId);
        prizeDTO.setCommissionId(commissionId);

        var savedPrizeClassification = new PrizeAssessmentClassification();
        savedPrizeClassification.setPrize(prize);
        savedPrizeClassification.setClassificationYear(classificationYear);
        savedPrizeClassification.setCommission(commission);

        when(prizeRepository.findById(prizeId)).thenReturn(Optional.of(prize));
        when(commissionService.findOne(commissionId)).thenReturn(commission);
        when(prizeAssessmentClassificationRepository.save(any(PrizeAssessmentClassification.class)))
            .thenReturn(savedPrizeClassification);
        doNothing().when(prizeAssessmentClassificationRepository)
            .deleteByPrizeIdAndCommissionId(prizeId, commissionId, true);
        when(prizeAssessmentClassificationRepository.save(any())).thenReturn(
            new PrizeAssessmentClassification() {{
                setAssessmentClassification(new AssessmentClassification() {{
                    setTitle(Collections.emptySet());
                }});
            }});

        // When
        var response =
            prizeAssessmentClassificationService.createPrizeAssessmentClassification(prizeDTO);

        // Then
        assertNotNull(response);
        verify(prizeAssessmentClassificationRepository).save(
            any(PrizeAssessmentClassification.class));
        verify(prizeService).reindexPrizeVolatileInformation(prize, null, false, true);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenCreatingWithNonExistentPrize() {
        // Given
        var prizeId = 999;
        var prizeDTO = new PrizeAssessmentClassificationDTO();
        prizeDTO.setPrizeId(prizeId);
        prizeDTO.setCommissionId(10);

        when(prizeRepository.findById(prizeId)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
            prizeAssessmentClassificationService.createPrizeAssessmentClassification(prizeDTO));
    }

    @Test
    void shouldThrowCantEditExceptionWhenPrizeHasNoAcquisitionDate() {
        // Given
        var prizeId = 1;
        var prizeDTO = new PrizeAssessmentClassificationDTO();
        prizeDTO.setPrizeId(prizeId);
        prizeDTO.setCommissionId(10);

        var prize = new Prize();
        prize.setId(prizeId);
        prize.setDate(null);

        when(prizeRepository.findById(prizeId)).thenReturn(Optional.of(prize));

        // When / Then
        assertThrows(CantEditException.class, () ->
            prizeAssessmentClassificationService.createPrizeAssessmentClassification(prizeDTO));
    }

    @Test
    void shouldDeleteExistingClassificationBeforeCreatingNewOne() {
        // Given
        var prizeId = 1;
        var commissionId = 10;
        var classificationYear = 2023;

        var prize = new Prize();
        prize.setId(prizeId);
        prize.setDate(LocalDate.of(classificationYear, 1, 1));

        var person = new Person();
        person.setId(100);
        prize.setPerson(person);

        var commission = new Commission();
        commission.setId(commissionId);

        var prizeDTO = new PrizeAssessmentClassificationDTO();
        prizeDTO.setPrizeId(prizeId);
        prizeDTO.setCommissionId(commissionId);

        when(prizeRepository.findById(prizeId)).thenReturn(Optional.of(prize));
        when(commissionService.findOne(commissionId)).thenReturn(commission);
        when(prizeAssessmentClassificationRepository.save(any(PrizeAssessmentClassification.class)))
            .thenReturn(new PrizeAssessmentClassification());
        when(prizeAssessmentClassificationRepository.save(any())).thenReturn(
            new PrizeAssessmentClassification() {{
                setAssessmentClassification(new AssessmentClassification() {{
                    setTitle(Collections.emptySet());
                }});
            }});
        when(assessmentClassificationService.findOne(any())).thenReturn(
            new AssessmentClassification());

        // When
        prizeAssessmentClassificationService.createPrizeAssessmentClassification(prizeDTO);

        // Then
        verify(prizeAssessmentClassificationRepository).deleteByPrizeIdAndCommissionId(prizeId,
            commissionId, true);
    }

    @Test
    void shouldPublishResearcherPointsReindexingEventAfterCreation() {
        // Given
        var prizeId = 1;
        var commissionId = 10;
        var classificationYear = 2024;

        var person = new Person();
        person.setId(100);

        var prize = new Prize();
        prize.setId(prizeId);
        prize.setDate(LocalDate.of(classificationYear, 1, 1));
        prize.setPerson(person);

        var commission = new Commission();
        commission.setId(commissionId);

        var prizeDTO = new PrizeAssessmentClassificationDTO();
        prizeDTO.setPrizeId(prizeId);
        prizeDTO.setCommissionId(commissionId);

        when(prizeRepository.findById(prizeId)).thenReturn(Optional.of(prize));
        when(commissionService.findOne(commissionId)).thenReturn(commission);
        when(prizeAssessmentClassificationRepository.save(any(PrizeAssessmentClassification.class)))
            .thenReturn(new PrizeAssessmentClassification());
        when(prizeAssessmentClassificationRepository.save(any())).thenReturn(
            new PrizeAssessmentClassification() {{
                setAssessmentClassification(new AssessmentClassification() {{
                    setTitle(Collections.emptySet());
                }});
            }});

        // When
        prizeAssessmentClassificationService.createPrizeAssessmentClassification(prizeDTO);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(
            any(ResearcherPointsReindexingEvent.class));
    }

    @Test
    void shouldEditPrizeAssessmentClassification() {
        // Given
        var classificationId = 1;
        var prizeId = 2;

        var prize = new Prize();
        prize.setId(prizeId);

        var person = new Person();
        person.setId(100);
        prize.setPerson(person);

        var existingClassification = new PrizeAssessmentClassification();
        existingClassification.setId(classificationId);
        existingClassification.setPrize(prize);

        var prizeDTO = new PrizeAssessmentClassificationDTO();
        prizeDTO.setPrizeId(prizeId);
        prizeDTO.setCommissionId(10);

        when(prizeAssessmentClassificationRepository.findById(classificationId))
            .thenReturn(Optional.of(existingClassification));
        when(
            entityAssessmentClassificationRepository.save(any(PrizeAssessmentClassification.class)))
            .thenReturn(existingClassification);

        // When
        prizeAssessmentClassificationService.editPrizeAssessmentClassification(classificationId,
            prizeDTO);

        // Then
        verify(entityAssessmentClassificationRepository).save(existingClassification);
        verify(prizeService).reindexPrizeVolatileInformation(prize, null, false, true);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenEditingNonExistentClassification() {
        // Given
        var classificationId = 999;
        var prizeDTO = new PrizeAssessmentClassificationDTO();

        when(prizeAssessmentClassificationRepository.findById(classificationId))
            .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
            prizeAssessmentClassificationService.editPrizeAssessmentClassification(classificationId,
                prizeDTO));
    }

    @Test
    void shouldPublishResearcherPointsReindexingEventAfterEditing() {
        // Given
        var classificationId = 1;
        var prizeId = 2;

        var person = new Person();
        person.setId(200);

        var prize = new Prize();
        prize.setId(prizeId);
        prize.setPerson(person);

        var existingClassification = new PrizeAssessmentClassification();
        existingClassification.setId(classificationId);
        existingClassification.setPrize(prize);

        var prizeDTO = new PrizeAssessmentClassificationDTO();
        prizeDTO.setPrizeId(prizeId);
        prizeDTO.setCommissionId(10);

        when(prizeAssessmentClassificationRepository.findById(classificationId))
            .thenReturn(Optional.of(existingClassification));
        when(
            entityAssessmentClassificationRepository.save(any(PrizeAssessmentClassification.class)))
            .thenReturn(existingClassification);

        // When
        prizeAssessmentClassificationService.editPrizeAssessmentClassification(classificationId,
            prizeDTO);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(
            any(ResearcherPointsReindexingEvent.class));
    }

    @Test
    void shouldSetCommonFieldsWhenEditingClassification() {
        // Given
        var classificationId = 1;
        var prizeId = 2;

        var prize = new Prize();
        prize.setId(prizeId);

        var person = new Person();
        person.setId(100);
        prize.setPerson(person);

        var existingClassification = new PrizeAssessmentClassification();
        existingClassification.setId(classificationId);
        existingClassification.setPrize(prize);

        var prizeDTO = new PrizeAssessmentClassificationDTO();
        prizeDTO.setPrizeId(prizeId);
        prizeDTO.setCommissionId(15);

        when(prizeAssessmentClassificationRepository.findById(classificationId))
            .thenReturn(Optional.of(existingClassification));
        when(
            entityAssessmentClassificationRepository.save(any(PrizeAssessmentClassification.class)))
            .thenReturn(existingClassification);

        // When
        prizeAssessmentClassificationService.editPrizeAssessmentClassification(classificationId,
            prizeDTO);

        // Then
        verify(entityAssessmentClassificationRepository).save(existingClassification);
    }

    @Test
    void shouldCallReindexingServiceAfterEditing() {
        // Given
        var classificationId = 1;
        var prizeId = 2;

        var prize = new Prize();
        prize.setId(prizeId);

        var person = new Person();
        person.setId(100);
        prize.setPerson(person);

        var existingClassification = new PrizeAssessmentClassification();
        existingClassification.setId(classificationId);
        existingClassification.setPrize(prize);

        var prizeDTO = new PrizeAssessmentClassificationDTO();
        prizeDTO.setPrizeId(prizeId);
        prizeDTO.setCommissionId(10);

        when(prizeAssessmentClassificationRepository.findById(classificationId))
            .thenReturn(Optional.of(existingClassification));
        when(
            entityAssessmentClassificationRepository.save(any(PrizeAssessmentClassification.class)))
            .thenReturn(existingClassification);

        // When
        prizeAssessmentClassificationService.editPrizeAssessmentClassification(classificationId,
            prizeDTO);

        // Then
        verify(prizeService).reindexPrizeVolatileInformation(prize, null, false, true);
    }
}
