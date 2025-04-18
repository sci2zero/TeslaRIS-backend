package rs.teslaris.core.unit.thesislibrary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.thesislibrary.dto.ThesisReportRequestDTO;
import rs.teslaris.thesislibrary.service.impl.ThesisLibraryReportingServiceImpl;

@SpringBootTest
public class ThesisLibraryReportingServiceTest {

    @Mock
    private ThesisRepository thesisRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @InjectMocks
    private ThesisLibraryReportingServiceImpl thesisLibraryReportingService;


    @Test
    void shouldReturnThesisReportCountsWhenDataExists() {
        // Given
        var request = new ThesisReportRequestDTO(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), List.of(1), ThesisType.PHD);

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1)).thenReturn(
            List.of(1, 2));

        when(thesisRepository.countDefendedThesesInPeriod(any(), any(), any(), any())).thenReturn(
            5);
        when(thesisRepository.countAcceptedThesesInPeriod(any(), any(), any(), any())).thenReturn(
            3);
        when(thesisRepository.countThesesWithPublicReviewInPeriod(any(), any(), any(),
            any())).thenReturn(2);
        when(
            thesisRepository.countPubliclyAvailableDefendedThesesThesesInPeriod(any(), any(), any(),
                any())).thenReturn(1);

        when(organisationUnitService.findOne(1)).thenReturn(new OrganisationUnit());

        // When
        var result = thesisLibraryReportingService.createThesisCountsReport(request);

        // Then
        assertEquals(1, result.size());
        var report = result.get(0);
        assertEquals(5, report.defendedCount());
        assertEquals(3, report.topicsAcceptedCount());
        assertEquals(2, report.putOnPublicReviewCount());
        assertEquals(1, report.publiclyAvailableCount());
    }

    @Test
    void shouldReturnEmptyListWhenNoDataExists() {
        // Given
        var request =
            new ThesisReportRequestDTO(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31),
                List.of(1), ThesisType.PHD);

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1)).thenReturn(
            List.of(1, 2));

        when(thesisRepository.countDefendedThesesInPeriod(any(), any(), any(), any())).thenReturn(
            0);
        when(thesisRepository.countAcceptedThesesInPeriod(any(), any(), any(), any())).thenReturn(
            0);
        when(thesisRepository.countThesesWithPublicReviewInPeriod(any(), any(), any(),
            any())).thenReturn(0);
        when(
            thesisRepository.countPubliclyAvailableDefendedThesesThesesInPeriod(any(), any(), any(),
                any())).thenReturn(0);

        // When
        var result = thesisLibraryReportingService.createThesisCountsReport(request);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyPageWhenNoDefendedThesesExist() {
        // Given
        var request = new ThesisReportRequestDTO(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), List.of(1), ThesisType.PHD);
        var pageable = Pageable.unpaged();

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
            .thenReturn(List.of(1, 2));

        when(documentPublicationIndexRepository.fetchDefendedThesesInPeriod(
            any(), any(), any(), any(), any())).thenReturn(Page.empty());

        // When
        var result = thesisLibraryReportingService.fetchDefendedThesesInPeriod(request, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyPageWhenNoAcceptedThesesExist() {
        // Given
        var request = new ThesisReportRequestDTO(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), List.of(1), ThesisType.PHD);
        var pageable = Pageable.unpaged();

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
            .thenReturn(List.of(1, 2));

        when(documentPublicationIndexRepository.fetchAcceptedThesesInPeriod(
            any(), any(), any(), any(), any())).thenReturn(Page.empty());

        // When
        var result = thesisLibraryReportingService.fetchAcceptedThesesInPeriod(request, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyPageWhenNoPublicReviewThesesExist() {
        // Given
        var request = new ThesisReportRequestDTO(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), List.of(1), ThesisType.PHD);
        var pageable = Pageable.unpaged();

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
            .thenReturn(List.of(1, 2));

        when(documentPublicationIndexRepository.fetchThesesWithPublicReviewInPeriod(
            any(), any(), any(), any(), any())).thenReturn(Page.empty());

        // When
        var result =
            thesisLibraryReportingService.fetchPublicReviewThesesInPeriod(request, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyPageWhenNoPubliclyAvailableThesesExist() {
        // Given
        var request = new ThesisReportRequestDTO(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), List.of(1), ThesisType.PHD);
        var pageable = Pageable.unpaged();

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
            .thenReturn(List.of(1, 2));

        when(documentPublicationIndexRepository.fetchPubliclyAvailableDefendedThesesInPeriod(
            any(), any(), any(), any(), any())).thenReturn(Page.empty());

        // When
        var result =
            thesisLibraryReportingService.fetchPubliclyAvailableThesesInPeriod(request, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnNonEmptyPageWhenDefendedThesesExist() {
        // Given
        var request = new ThesisReportRequestDTO(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), List.of(1), ThesisType.PHD);
        var pageable = Pageable.unpaged();
        var thesisIndex = new DocumentPublicationIndex();
        var expectedPage = new PageImpl<>(List.of(thesisIndex));

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
            .thenReturn(List.of(1, 2));

        when(documentPublicationIndexRepository.fetchDefendedThesesInPeriod(
            any(), any(), any(), any(), any())).thenReturn(expectedPage);

        // When
        var result = thesisLibraryReportingService.fetchDefendedThesesInPeriod(request, pageable);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
    }
}
