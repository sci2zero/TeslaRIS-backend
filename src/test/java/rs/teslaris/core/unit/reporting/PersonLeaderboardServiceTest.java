package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.reporting.service.impl.PersonLeaderboardServiceImpl;

@SpringBootTest
public class PersonLeaderboardServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private PersonLeaderboardServiceImpl service;


    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnTopResearchersByPublicationCount() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        var toYear = 2023;

        var mockPerson1 = new PersonIndex();
        mockPerson1.setDatabaseId(1);
        var mockPerson2 = new PersonIndex();
        mockPerson2.setDatabaseId(2);

        var mockPersonHit1 = mock(Hit.class);
        when(mockPersonHit1.source()).thenReturn(mockPerson1);
        var mockPersonHit2 = mock(Hit.class);
        when(mockPersonHit2.source()).thenReturn(mockPerson2);

        var mockPersonResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockPersonResponse.hits().hits()).thenReturn(
            Arrays.asList(mockPersonHit1, mockPersonHit2));

        var mockBucket1 = mock(StringTermsBucket.class);
        when(mockBucket1.key()).thenReturn(FieldValue.of("1"));
        when(mockBucket1.docCount()).thenReturn(15L);

        var mockBucket2 = mock(StringTermsBucket.class);
        when(mockBucket2.key()).thenReturn(FieldValue.of("2"));
        when(mockBucket2.docCount()).thenReturn(10L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.sterms().buckets().array()).thenReturn(
            Arrays.asList(mockBucket1, mockBucket2));

        var mockPublicationResponse = mock(SearchResponse.class);
        var mockAggs = mock(Map.class);
        when(mockPublicationResponse.aggregations()).thenReturn(mockAggs);
        when(mockPublicationResponse.aggregations().get("by_person")).thenReturn(mockAgg);

        var mockDetailedPerson1 = new PersonIndex();
        mockDetailedPerson1.setDatabaseId(1);
        mockDetailedPerson1.setName("John Doe");

        var mockDetailedPerson2 = new PersonIndex();
        mockDetailedPerson2.setDatabaseId(2);
        mockDetailedPerson2.setName("Jane Smith");

        var mockDetailedHit1 = mock(Hit.class);
        when(mockDetailedHit1.source()).thenReturn(mockDetailedPerson1);
        var mockDetailedHit2 = mock(Hit.class);
        when(mockDetailedHit2.source()).thenReturn(mockDetailedPerson2);

        var mockDetailedPersonResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockDetailedPersonResponse.hits().hits()).thenReturn(
            Arrays.asList(mockDetailedHit1, mockDetailedHit2));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenReturn(mockPersonResponse, mockDetailedPersonResponse);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class)
        )).thenReturn(mockPublicationResponse);

        // When
        var result = service.getTopResearchersByPublicationCount(institutionId, fromYear, toYear);

        // Then
        assertEquals(2, result.size());
        assertEquals(15L, result.get(mockDetailedPerson1));
        assertEquals(10L, result.get(mockDetailedPerson2));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenNoEligiblePersons() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        var toYear = 2023;

        var mockPersonResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockPersonResponse.hits().hits()).thenReturn(Collections.emptyList());

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenReturn(mockPersonResponse);

        // When
        var result = service.getTopResearchersByPublicationCount(institutionId, fromYear, toYear);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenNoPublicationsFound() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        var toYear = 2023;

        var mockPerson = new PersonIndex();
        mockPerson.setDatabaseId(1);

        var mockPersonHit = mock(Hit.class);
        when(mockPersonHit.source()).thenReturn(mockPerson);

        var mockPersonResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockPersonResponse.hits().hits()).thenReturn(List.of(mockPersonHit));

        var mockPublicationResponse = mock(SearchResponse.class);
        var mockAggs = mock(Map.class);
        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.sterms().buckets().array()).thenReturn(List.of());
        when(mockPublicationResponse.aggregations()).thenReturn(mockAggs);
        when(mockPublicationResponse.aggregations().get("by_person")).thenReturn(
            mockAgg); // No aggregations

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenReturn(mockPersonResponse);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class)
        )).thenReturn(mockPublicationResponse);

        // When
        var result = service.getTopResearchersByPublicationCount(institutionId, fromYear, toYear);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnResearchersWithMostCitations() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        var toYear = 2023;

        var mockPerson1 = new PersonIndex();
        mockPerson1.setDatabaseId(1);
        mockPerson1.setName("Researcher 1");
        mockPerson1.setCitationsByYear(Map.of(2020, 10, 2021, 15, 2022, 20, 2023, 5)); // Total: 50

        var mockPerson2 = new PersonIndex();
        mockPerson2.setDatabaseId(2);
        mockPerson2.setName("Researcher 2");
        mockPerson2.setCitationsByYear(Map.of(2021, 25, 2022, 30)); // Total: 55

        var mockPerson3 = new PersonIndex();
        mockPerson3.setDatabaseId(3);
        mockPerson3.setName("Researcher 3");
        mockPerson3.setCitationsByYear(Map.of(2019, 100)); // Outside range, should be filtered out

        var mockHit1 = mock(Hit.class);
        when(mockHit1.source()).thenReturn(mockPerson1);
        var mockHit2 = mock(Hit.class);
        when(mockHit2.source()).thenReturn(mockPerson2);
        var mockHit3 = mock(Hit.class);
        when(mockHit3.source()).thenReturn(mockPerson3);

        var mockResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockResponse.hits().hits()).thenReturn(Arrays.asList(mockHit1, mockHit2, mockHit3));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            any()
        )).thenReturn(mockResponse);

        // When
        var result = service.getResearchersWithMostCitations(institutionId, fromYear, toYear);

        // Then
        assertEquals(2, result.size());

        var entries = new ArrayList<>(result.entrySet());
        assertEquals(mockPerson2, entries.get(0).getKey());
        assertEquals(55L, entries.get(0).getValue());
        assertEquals(mockPerson1, entries.get(1).getKey());
        assertEquals(50L, entries.get(1).getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterOutResearchersWithNoCitationsInRange() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        var toYear = 2023;

        var mockPerson1 = new PersonIndex();
        mockPerson1.setDatabaseId(1);
        mockPerson1.setName("Researcher 1");
        mockPerson1.setCitationsByYear(Map.of(2020, 10, 2021, 15)); // Within range

        var mockPerson2 = new PersonIndex();
        mockPerson2.setDatabaseId(2);
        mockPerson2.setName("Researcher 2");
        mockPerson2.setCitationsByYear(Map.of(2018, 20, 2019, 30)); // Outside range

        var mockPerson3 = new PersonIndex();
        mockPerson3.setDatabaseId(3);
        mockPerson3.setName("Researcher 3");
        mockPerson3.setCitationsByYear(null); // No citations

        var mockHit1 = mock(Hit.class);
        when(mockHit1.source()).thenReturn(mockPerson1);
        var mockHit2 = mock(Hit.class);
        when(mockHit2.source()).thenReturn(mockPerson2);
        var mockHit3 = mock(Hit.class);
        when(mockHit3.source()).thenReturn(mockPerson3);

        var mockResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockResponse.hits().hits()).thenReturn(Arrays.asList(mockHit1, mockHit2, mockHit3));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenReturn(mockResponse);

        // When
        var result = service.getResearchersWithMostCitations(institutionId, fromYear, toYear);

        // Then
        assertEquals(1, result.size());
        assertEquals(mockPerson1, result.keySet().iterator().next());
        assertEquals(25L, result.get(mockPerson1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenIOExceptionOccursInPublicationCount() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        var toYear = 2023;

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            any()
        )).thenThrow(new IOException("Connection failed"));

        // When
        var result = service.getTopResearchersByPublicationCount(institutionId, fromYear, toYear);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenIOExceptionOccursInCitationCount() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        var toYear = 2023;

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenThrow(new IOException("Connection failed"));

        // When
        var result = service.getResearchersWithMostCitations(institutionId, fromYear, toYear);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldLimitToTop10ResearchersInCitationCount() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        var toYear = 2023;

        List<Hit<PersonIndex>> hits = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            var mockPerson = new PersonIndex();
            mockPerson.setDatabaseId(i);
            mockPerson.setName("Researcher " + i);
            mockPerson.setCitationsByYear(Map.of(2022, i * 10)); // Different citation counts

            var mockHit = mock(Hit.class);
            when(mockHit.source()).thenReturn(mockPerson);
            hits.add(mockHit);
        }

        var mockResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockResponse.hits().hits()).thenReturn(hits);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenReturn(mockResponse);

        // When
        var result = service.getResearchersWithMostCitations(institutionId, fromYear, toYear);

        // Then
        assertEquals(10, result.size());

        long previousCount = Long.MAX_VALUE;
        for (long count : result.values()) {
            assertTrue(count <= previousCount);
            previousCount = count;
        }
    }
}
