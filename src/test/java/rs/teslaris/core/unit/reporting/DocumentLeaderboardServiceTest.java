package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.reporting.service.impl.DocumentLeaderboardServiceImpl;

@SpringBootTest
public class DocumentLeaderboardServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private DocumentLeaderboardServiceImpl documentLeaderboardService;


    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnPublicationsWithMostCitations() throws IOException {
        // Given
        var institutionId = 1;
        var fromYear = 2020;
        var toYear = 2024;

        var publication1 = new DocumentPublicationIndex();
        publication1.setTotalCitations(15L);
        var hit1 = mock(Hit.class);
        when(hit1.source()).thenReturn(publication1);

        var publication2 = new DocumentPublicationIndex();
        publication2.setTotalCitations(30L);
        var hit2 = mock(Hit.class);
        when(hit2.source()).thenReturn(publication2);

        var mockHits = List.of(hit1, hit2);

        var mockHitsMetadata = mock(HitsMetadata.class);
        when(mockHitsMetadata.hits()).thenReturn(mockHits);

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.hits()).thenReturn(mockHitsMetadata);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(DocumentPublicationIndex.class))
        ).thenReturn(mockResponse);

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(List.of(institutionId));

        // When
        var result =
            documentLeaderboardService.getPublicationsWithMostCitations(institutionId, fromYear,
                toYear);

        // Then
        assertEquals(2, result.size());
        assertEquals(15L, result.get(0).b);
        assertEquals(30L, result.get(1).b);
        assertNotNull(result.get(0).a);
        assertNotNull(result.get(1).a);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotReturnEmptyListWhenYearRangeInvalid() throws IOException {
        // Given
        var institutionId = 1;
        Integer fromYear = null;
        Integer toYear = null;

        var publication1 = new DocumentPublicationIndex();
        publication1.setTotalCitations(15L);
        var hit1 = mock(Hit.class);
        when(hit1.source()).thenReturn(publication1);

        var mockHits = List.of(hit1);

        var mockHitsMetadata = mock(HitsMetadata.class);
        when(mockHitsMetadata.hits()).thenReturn(mockHits);

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.hits()).thenReturn(mockHitsMetadata);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(DocumentPublicationIndex.class))
        ).thenReturn(mockResponse);

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(List.of(institutionId));

        // When
        var result =
            documentLeaderboardService.getPublicationsWithMostCitations(institutionId, fromYear,
                toYear);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenIOExceptionOccurs() throws IOException {
        // Given
        var institutionId = 1;
        var fromYear = 2020;
        var toYear = 2024;

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(DocumentPublicationIndex.class))
        ).thenThrow(new IOException("Connection error"));

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(List.of(institutionId));

        // When
        var result =
            documentLeaderboardService.getPublicationsWithMostCitations(institutionId, fromYear,
                toYear);

        // Then
        assertTrue(result.isEmpty());
    }
}
