package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.reporting.service.impl.PersonLeaderboardServiceImpl;
import rs.teslaris.reporting.utility.QueryUtil;

@SpringBootTest
public class PersonLeaderboardServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private PersonIndexRepository personIndexRepository;

    @InjectMocks
    private PersonLeaderboardServiceImpl service;


    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnTopResearchersByPublicationCount() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(
                    () -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids", "organisation_unit_ids_active"));

            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(institutionId))
                .thenReturn(List.of(123, 456, 789));

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

            var mockBucket1 = mock(LongTermsBucket.class);
            when(mockBucket1.key()).thenReturn(1L);
            when(mockBucket1.docCount()).thenReturn(15L);

            var mockBucket2 = mock(LongTermsBucket.class);
            when(mockBucket2.key()).thenReturn(2L);
            when(mockBucket2.docCount()).thenReturn(10L);

            var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(mockAgg.lterms().buckets().array()).thenReturn(
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
            var result =
                service.getTopResearchersByPublicationCount(institutionId, fromYear, toYear);

            // Then
            assertEquals(2, result.size());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenNoEligiblePersons() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(
                    () -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids", "organisation_unit_ids_active"));

            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(institutionId))
                .thenReturn(List.of(123, 456, 789));

            var mockPersonResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            when(mockPersonResponse.hits().hits()).thenReturn(Collections.emptyList());

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(PersonIndex.class)
            )).thenReturn(mockPersonResponse);

            // When
            var result =
                service.getTopResearchersByPublicationCount(institutionId, fromYear, toYear);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenNoPublicationsFound() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(
                    () -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids", "organisation_unit_ids_active"));

            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(institutionId))
                .thenReturn(List.of(123, 456, 789));

            var mockPerson = new PersonIndex();
            mockPerson.setDatabaseId(1);

            var mockPersonHit = mock(Hit.class);
            when(mockPersonHit.source()).thenReturn(mockPerson);

            var mockPersonResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            when(mockPersonResponse.hits().hits()).thenReturn(List.of(mockPersonHit));

            var mockPublicationResponse = mock(SearchResponse.class);
            var mockAggs = mock(Map.class);
            var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(mockAgg.lterms().buckets().array()).thenReturn(List.of());
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
            var result =
                service.getTopResearchersByPublicationCount(institutionId, fromYear, toYear);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterOutResearchersWithNoCitationsInRange() throws IOException {
        // Given
        var institutionId = 123;

        LongTermsBucket bucket1 = mock(LongTermsBucket.class);
        when(bucket1.key()).thenReturn(1L);
        Aggregate totalCitationsAgg1 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        SumAggregate sumAgg1 = mock(SumAggregate.class);
        when(sumAgg1.value()).thenReturn(25.0);
        when(totalCitationsAgg1.sum()).thenReturn(sumAgg1);
        when(bucket1.aggregations()).thenReturn(Map.of("total_citations", totalCitationsAgg1));

        LongTermsBucket bucket2 = mock(LongTermsBucket.class);
        when(bucket2.key()).thenReturn(2L);
        Aggregate totalCitationsAgg2 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        SumAggregate sumAgg2 = mock(SumAggregate.class);
        when(sumAgg2.value()).thenReturn(0.0);
        when(totalCitationsAgg2.sum()).thenReturn(sumAgg2);
        when(bucket2.aggregations()).thenReturn(Map.of("total_citations", totalCitationsAgg2));

        Aggregate byPersonAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(byPersonAgg.lterms().buckets().array()).thenReturn(List.of(bucket1, bucket2));

        Map<String, Aggregate> topAggs = Map.of("by_person", byPersonAgg);

        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(topAggs);

        when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
            .thenReturn(mockResponse);

        PersonIndex person1 = new PersonIndex();
        person1.setDatabaseId(1);
        person1.setName("Researcher 1");
        when(personIndexRepository.findByDatabaseId(1)).thenReturn(Optional.of(person1));

        PersonIndex person2 = new PersonIndex();
        person2.setDatabaseId(2);
        person2.setName("Researcher 2");
        when(personIndexRepository.findByDatabaseId(2)).thenReturn(Optional.of(person2));

        // When
        var result = service.getResearchersWithMostCitations(institutionId, 2020, 2023);

        // Then
        assertEquals(2, result.size());
        assertEquals(person1, result.getFirst().a);
        assertEquals(25L, result.getFirst().b);
    }


    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenIOExceptionOccursInPublicationCount() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(
                    () -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids", "organisation_unit_ids_active"));

            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(institutionId))
                .thenReturn(List.of(123, 456, 789));

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                any()
            )).thenThrow(new IOException("Connection failed"));

            // When
            var result =
                service.getTopResearchersByPublicationCount(institutionId, fromYear, toYear);

            // Then
            assertTrue(result.isEmpty());
        }
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
            eq(Void.class)
        )).thenThrow(new IOException("Connection failed"));

        // When
        var result = service.getResearchersWithMostCitations(institutionId, fromYear, toYear);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnResearchersWithMostCitations() throws IOException {
        // Given
        var institutionId = 123;

        var bucket1 = mock(LongTermsBucket.class);
        when(bucket1.key()).thenReturn(1L);
        var totalCitations1 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        var sum1 = mock(SumAggregate.class);
        when(sum1.value()).thenReturn(50.0);
        when(totalCitations1.sum()).thenReturn(sum1);
        when(bucket1.aggregations()).thenReturn(Map.of("total_citations", totalCitations1));

        var bucket2 = mock(LongTermsBucket.class);
        when(bucket2.key()).thenReturn(2L);
        var totalCitations2 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        var sum2 = mock(SumAggregate.class);
        when(sum2.value()).thenReturn(55.0);
        when(totalCitations2.sum()).thenReturn(sum2);
        when(bucket2.aggregations()).thenReturn(Map.of("total_citations", totalCitations2));

        Aggregate byPersonAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(byPersonAgg.lterms().buckets().array()).thenReturn(List.of(bucket1, bucket2));
        Map<String, Aggregate> topAggs = Map.of("by_person", byPersonAgg);

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(topAggs);

        var person1 = new PersonIndex();
        person1.setDatabaseId(1);
        person1.setName("Researcher 1");
        var person2 = new PersonIndex();
        person2.setDatabaseId(2);
        person2.setName("Researcher 2");

        when(elasticsearchClient.search(any(Function.class), eq(Void.class))).thenReturn(
            mockResponse);
        when(personIndexRepository.findByDatabaseId(1)).thenReturn(Optional.of(person1));
        when(personIndexRepository.findByDatabaseId(2)).thenReturn(Optional.of(person2));

        // When
        var result = service.getResearchersWithMostCitations(institutionId, 2020, 2023);

        // Then
        assertEquals(2, result.size());
        assertEquals(person2, result.getFirst().a);
        assertEquals(55L, result.getFirst().b);
        assertEquals(person1, result.get(1).a);
        assertEquals(50L, result.get(1).b);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldLimitToTop10ResearchersInCitationCount() throws IOException {
        // Given
        var institutionId = 123;

        List<LongTermsBucket> buckets = new ArrayList<>();
        Map<Integer, PersonIndex> personMap = new HashMap<>();
        for (int i = 1; i <= 15; i++) {
            LongTermsBucket bucket = mock(LongTermsBucket.class);
            when(bucket.key()).thenReturn((long) i);
            Aggregate totalAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            SumAggregate sum = mock(SumAggregate.class);
            when(sum.value()).thenReturn((double) (i * 10));
            when(totalAgg.sum()).thenReturn(sum);
            when(bucket.aggregations()).thenReturn(Map.of("total_citations", totalAgg));
            buckets.add(bucket);

            PersonIndex person = new PersonIndex();
            person.setDatabaseId(i);
            person.setName("Researcher " + i);
            personMap.put(i, person);
            when(personIndexRepository.findByDatabaseId(i)).thenReturn(Optional.of(person));
        }

        var byPersonAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(byPersonAgg.lterms().buckets().array()).thenReturn(buckets);
        var topAggs = Map.of("by_person", byPersonAgg);

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(topAggs);
        when(elasticsearchClient.search(any(Function.class), eq(Void.class))).thenReturn(
            mockResponse);

        // When
        var result = service.getResearchersWithMostCitations(institutionId, 2020, 2023);

        // Then
        assertEquals(10, result.size());
        long previousCount = Long.MAX_VALUE;
        for (var entry : result) {
            assertTrue(entry.b <= previousCount);
            previousCount = entry.b;
        }
    }
}
