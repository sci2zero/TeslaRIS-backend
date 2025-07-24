package rs.teslaris.core.unit.thesislibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.thesislibrary.service.impl.ThesisLibraryDissertationReportServiceImpl;

@SpringBootTest
class ThesisLibraryDissertationReportServiceTest {

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private SearchService<DocumentPublicationIndex> searchService;

    @InjectMocks
    private ThesisLibraryDissertationReportServiceImpl service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "daysOnPublicReview", 30);
    }

    @Test
    void testFetchPublicReviewDissertations_WithInstitutionAndYear() {
        // Given
        var institutionId = 123;
        var year = 2023;
        var notDefendedOnly = false;
        var pageable = PageRequest.of(0, 10);

        var subHierarchyIds = Set.of(123, 456);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(List.copyOf(subHierarchyIds));

        DocumentPublicationIndex mockIndex = new DocumentPublicationIndex();
        mockIndex.setThesisAuthorName("Author A");
        mockIndex.setTitleSr("Naslov");
        mockIndex.setTitleOther("Title");
        mockIndex.setThesisInstitutionNameSr("Fakultet");
        mockIndex.setThesisInstitutionNameOther("Faculty");
        mockIndex.setScientificFieldSr("CS");
        mockIndex.setLatestPublicReviewStartDate(LocalDate.of(2023, 6, 1));
        mockIndex.setDatabaseId(123);

        var mockPage = new PageImpl<>(List.of(mockIndex));
        when(searchService.runQuery(any(), any(), eq(DocumentPublicationIndex.class),
            eq("document_publication")))
            .thenReturn(mockPage);

        // When
        var result = service.fetchPublicReviewDissertations(
            institutionId, year, notDefendedOnly, pageable
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        var dto = result.getContent().getFirst();
        assertThat(dto.nameAndSurname()).isEqualTo("Author A");
        assertThat(dto.titleSr()).isEqualTo("Naslov");
        assertThat(dto.titleOther()).isEqualTo("Title");
        assertThat(dto.publicReviewEndDate()).isEqualTo("2023-07-01"); // 30 days added
    }

    @Test
    void testFetchPublicReviewDissertations_NotDefendedOnlyTrue() {
        // Given
        Integer institutionId = null;
        Integer year = null;
        var notDefendedOnly = true;
        var pageable = PageRequest.of(0, 5);

        DocumentPublicationIndex mockIndex = new DocumentPublicationIndex();
        mockIndex.setAuthorNames("Author X");
        mockIndex.setTitleSr("Tema");
        mockIndex.setTitleOther("Topic");
        mockIndex.setThesisInstitutionNameSr("Institut");
        mockIndex.setThesisInstitutionNameOther("Institute");
        mockIndex.setScientificFieldSr("Math");
        mockIndex.setLatestPublicReviewStartDate(LocalDate.now().minusDays(10));
        mockIndex.setDatabaseId(456);

        var mockPage = new PageImpl<>(List.of(mockIndex));
        when(searchService.runQuery(any(), any(), eq(DocumentPublicationIndex.class),
            eq("document_publication")))
            .thenReturn(mockPage);

        // When
        var result = service.fetchPublicReviewDissertations(
            institutionId, year, notDefendedOnly, pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void testFetchPublicReviewDissertations_NoFilters() {
        // When
        when(searchService.runQuery(any(), any(), eq(DocumentPublicationIndex.class),
            eq("document_publication")))
            .thenReturn(Page.empty());

        var result = service.fetchPublicReviewDissertations(null, null, null,
            PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }
}

