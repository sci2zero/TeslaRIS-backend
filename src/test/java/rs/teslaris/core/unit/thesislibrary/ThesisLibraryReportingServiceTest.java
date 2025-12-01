package rs.teslaris.core.unit.thesislibrary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.thesislibrary.dto.NotAddedToPromotionThesesRequestDTO;
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

    @Mock
    private OrganisationUnitIndexRepository organisationUnitIndexRepository;

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
        when(organisationUnitIndexRepository.findOrganisationUnitIndexesBySuperOUId(anyInt(),
            eq(Pageable.unpaged()))).thenReturn(
            new PageImpl<>(List.of(new OrganisationUnitIndex())));

        // When
        var result = thesisLibraryReportingService.createThesisCountsReport(request);

        // Then
        assertEquals(1, result.size());
        var report = result.getFirst();
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
        when(organisationUnitIndexRepository.findOrganisationUnitIndexesBySuperOUId(anyInt(),
            eq(Pageable.unpaged()))).thenReturn(
            new PageImpl<>(List.of(new OrganisationUnitIndex())));

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

    @Test
    void shouldFetchThesesWithLibraryInstitutionIdProvided() {
        // Given
        var request = new NotAddedToPromotionThesesRequestDTO(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
            List.of(),
            List.of(ThesisType.PHD, ThesisType.PHD_ART_PROJECT)
        );

        var libraryInstitutionId = 1;
        var expectedInstitutionIds = List.of(1, 2, 3);
        var expectedThesisTypes = List.of("PHD", "PHD_ART_PROJECT");
        var pageable = PageRequest.of(0, 10);
        var expectedPage = new PageImpl<>(List.of(new DocumentPublicationIndex()));

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(libraryInstitutionId))
            .thenReturn(expectedInstitutionIds);
        when(documentPublicationIndexRepository.fetchDefendedThesesNotSentToPromotionInPeriod(
            eq(request.fromDate()),
            eq(request.toDate()),
            eq(expectedInstitutionIds),
            eq(expectedThesisTypes),
            eq(pageable)
        )).thenReturn(expectedPage);

        // When
        var result = thesisLibraryReportingService.fetchDefendedThesesInPeriodNotSentToPromotion(
            request, libraryInstitutionId, pageable);

        // Then
        verify(organisationUnitService).getOrganisationUnitIdsFromSubHierarchy(
            libraryInstitutionId);
        verify(documentPublicationIndexRepository).fetchDefendedThesesNotSentToPromotionInPeriod(
            request.fromDate(),
            request.toDate(),
            expectedInstitutionIds,
            expectedThesisTypes,
            pageable
        );
        assertEquals(expectedPage, result);
    }

    @Test
    void shouldFetchThesesWithTopLevelInstitutionsWhenLibraryInstitutionIdIsNull() {
        // Given
        var topLevelInstitutionIds = List.of(10, 20);
        var request = new NotAddedToPromotionThesesRequestDTO(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
            topLevelInstitutionIds,
            List.of(ThesisType.PHD, ThesisType.PHD_ART_PROJECT)
        );

        var expectedInstitutionIds = List.of(20, 21, 22, 10, 11, 12);
        var pageable = PageRequest.of(1, 20);
        var expectedPage = new PageImpl<>(List.of(
            new DocumentPublicationIndex(),
            new DocumentPublicationIndex()
        ));

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(10))
            .thenReturn(List.of(10, 11, 12));
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(20))
            .thenReturn(List.of(20, 21, 22));
        when(documentPublicationIndexRepository.fetchDefendedThesesNotSentToPromotionInPeriod(
            eq(request.fromDate()),
            eq(request.toDate()),
            eq(expectedInstitutionIds),
            eq(List.of("PHD", "PHD_ART_PROJECT")),
            eq(pageable)
        )).thenReturn(expectedPage);

        // When
        var result = thesisLibraryReportingService.fetchDefendedThesesInPeriodNotSentToPromotion(
            request, null, pageable);

        // Then
        verify(organisationUnitService, never()).getOrganisationUnitIdsFromSubHierarchy(isNull());
        verify(organisationUnitService).getOrganisationUnitIdsFromSubHierarchy(10);
        verify(organisationUnitService).getOrganisationUnitIdsFromSubHierarchy(20);
        verify(documentPublicationIndexRepository).fetchDefendedThesesNotSentToPromotionInPeriod(
            request.fromDate(),
            request.toDate(),
            expectedInstitutionIds,
            List.of("PHD", "PHD_ART_PROJECT"),
            pageable
        );
        assertEquals(2, result.getContent().size());
        assertEquals(expectedPage, result);
    }

    @Test
    void shouldUseDefaultThesisTypesWhenEmptyListProvided() {
        // Given
        var request = new NotAddedToPromotionThesesRequestDTO(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
            List.of(100),
            List.of()
        );

        var libraryInstitutionId = 1;
        var expectedInstitutionIds = List.of(1, 2, 3);
        var expectedDefaultThesisTypes = List.of("PHD", "PHD_ART_PROJECT");
        var pageable = PageRequest.of(0, 50);
        var expectedPage = new PageImpl<>(List.of(new DocumentPublicationIndex()));

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(libraryInstitutionId))
            .thenReturn(expectedInstitutionIds);
        when(documentPublicationIndexRepository.fetchDefendedThesesNotSentToPromotionInPeriod(
            eq(request.fromDate()),
            eq(request.toDate()),
            eq(expectedInstitutionIds),
            eq(expectedDefaultThesisTypes),
            eq(pageable)
        )).thenReturn(expectedPage);

        // When
        var result = thesisLibraryReportingService.fetchDefendedThesesInPeriodNotSentToPromotion(
            request, libraryInstitutionId, pageable);

        // Then
        verify(documentPublicationIndexRepository).fetchDefendedThesesNotSentToPromotionInPeriod(
            request.fromDate(),
            request.toDate(),
            expectedInstitutionIds,
            expectedDefaultThesisTypes,
            pageable
        );
        assertEquals(expectedPage, result);
        assertTrue(request.thesisTypes().isEmpty());
    }
}
