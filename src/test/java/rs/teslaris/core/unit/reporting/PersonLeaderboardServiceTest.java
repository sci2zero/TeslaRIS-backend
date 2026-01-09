package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.impl.leaderboards.PersonLeaderboardServiceImpl;
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

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));

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
            when(mockAggs.get("by_person")).thenReturn(mockAgg);

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
            when(mockPublicationResponse.aggregations().get("by_all_persons")).thenReturn(
                mockAgg);
            when(mockAgg.filter().aggregations()).thenReturn(mockAggs);

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

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));

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

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));

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
            when(mockAggs.get("by_person")).thenReturn(
                mockAgg); // No aggregations
            when(mockPublicationResponse.aggregations().get("by_all_persons")).thenReturn(
                mockAgg);
            when(mockAgg.filter().aggregations()).thenReturn(mockAggs);

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

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));

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

        SearchResponse<PersonIndex> mockPersonResponse =
            mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        var mockHit = mock(Hit.class);
        when(mockHit.source()).thenReturn(new PersonIndex() {{
            setDatabaseId(1);
        }});
        when(mockPersonResponse.hits().hits()).thenReturn(List.of(mockHit));
        when(elasticsearchClient.search(any(Function.class), eq(PersonIndex.class)))
            .thenReturn(mockPersonResponse);

        // When
        var result = service.getResearchersWithMostCitations(institutionId, fromYear, toYear);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterOutResearchersWithNoCitationsInRange() throws IOException {
        // Given
        var institutionId = 123;

        LongTermsBucket bucket1 = mock(LongTermsBucket.class);
        when(bucket1.key()).thenReturn(1L);
        Map<String, Aggregate> aggs1 = new HashMap<>();
        for (int y = 2020; y <= 2023; y++) {
            Aggregate agg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            SumAggregate sum = mock(SumAggregate.class);
            when(sum.value()).thenReturn(y == 2020 ? 25.0 : 0.0);
            when(agg.sum()).thenReturn(sum);
            aggs1.put("year_" + y, agg);
        }
        when(bucket1.aggregations()).thenReturn(aggs1);

        LongTermsBucket bucket2 = mock(LongTermsBucket.class);
        when(bucket2.key()).thenReturn(2L);
        Map<String, Aggregate> aggs2 = new HashMap<>();
        for (int y = 2020; y <= 2023; y++) {
            Aggregate agg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            SumAggregate sum = mock(SumAggregate.class);
            when(sum.value()).thenReturn(0.0);
            when(agg.sum()).thenReturn(sum);
            aggs2.put("year_" + y, agg);
        }
        when(bucket2.aggregations()).thenReturn(aggs2);

        Aggregate byPersonAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(byPersonAgg.lterms().buckets().array()).thenReturn(List.of(bucket1, bucket2));
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(Map.of("by_person", byPersonAgg));
        when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
            .thenReturn(mockResponse);

        SearchResponse<PersonIndex> mockPersonResponse =
            mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        var mockHit = mock(Hit.class);
        when(mockHit.source()).thenReturn(new PersonIndex() {{
            setDatabaseId(1);
        }});
        when(mockPersonResponse.hits().hits()).thenReturn(List.of(mockHit));
        when(elasticsearchClient.search(any(Function.class), eq(PersonIndex.class)))
            .thenReturn(mockPersonResponse);

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
        assertEquals(1, result.size());
        assertEquals(person1, result.getFirst().a);
        assertEquals(25L, result.getFirst().b);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnResearchersWithMostCitations() throws IOException {
        // Given
        var institutionId = 123;

        var bucket1 = mock(LongTermsBucket.class);
        when(bucket1.key()).thenReturn(1L);
        Map<String, Aggregate> aggs1 = new HashMap<>();
        for (int y = 2020; y <= 2023; y++) {
            Aggregate agg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            SumAggregate sum = mock(SumAggregate.class);
            when(sum.value()).thenReturn(y == 2020 ? 50.0 : 0.0);
            when(agg.sum()).thenReturn(sum);
            aggs1.put("year_" + y, agg);
        }
        when(bucket1.aggregations()).thenReturn(aggs1);

        var bucket2 = mock(LongTermsBucket.class);
        when(bucket2.key()).thenReturn(2L);
        Map<String, Aggregate> aggs2 = new HashMap<>();
        for (int y = 2020; y <= 2023; y++) {
            Aggregate agg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            SumAggregate sum = mock(SumAggregate.class);
            when(sum.value()).thenReturn(y == 2020 ? 55.0 : 0.0);
            when(agg.sum()).thenReturn(sum);
            aggs2.put("year_" + y, agg);
        }
        when(bucket2.aggregations()).thenReturn(aggs2);

        Aggregate byPersonAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(byPersonAgg.lterms().buckets().array()).thenReturn(List.of(bucket1, bucket2));
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(Map.of("by_person", byPersonAgg));
        when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
            .thenReturn(mockResponse);

        SearchResponse<PersonIndex> mockPersonResponse =
            mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        var mockHit = mock(Hit.class);
        when(mockHit.source()).thenReturn(new PersonIndex() {{
            setDatabaseId(1);
        }});
        when(mockPersonResponse.hits().hits()).thenReturn(List.of(mockHit));
        when(elasticsearchClient.search(any(Function.class), eq(PersonIndex.class)))
            .thenReturn(mockPersonResponse);

        var person1 = new PersonIndex();
        person1.setDatabaseId(1);
        person1.setName("Researcher 1");
        var person2 = new PersonIndex();
        person2.setDatabaseId(2);
        person2.setName("Researcher 2");
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
        for (int i = 1; i <= 15; i++) {
            LongTermsBucket bucket = mock(LongTermsBucket.class);
            when(bucket.key()).thenReturn((long) i);
            Map<String, Aggregate> aggs = new HashMap<>();
            for (int y = 2020; y <= 2023; y++) {
                Aggregate agg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
                SumAggregate sum = mock(SumAggregate.class);
                when(sum.value()).thenReturn((double) (i * 10));
                when(agg.sum()).thenReturn(sum);
                aggs.put("year_" + y, agg);
            }
            when(bucket.aggregations()).thenReturn(aggs);
            buckets.add(bucket);

            PersonIndex person = new PersonIndex();
            person.setDatabaseId(i);
            person.setName("Researcher " + i);
            when(personIndexRepository.findByDatabaseId(i)).thenReturn(Optional.of(person));
        }

        var byPersonAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(byPersonAgg.lterms().buckets().array()).thenReturn(buckets);
        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(Map.of("by_person", byPersonAgg));
        when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
            .thenReturn(mockResponse);

        SearchResponse<PersonIndex> mockPersonResponse =
            mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        var mockHit = mock(Hit.class);
        when(mockHit.source()).thenReturn(new PersonIndex() {{
            setDatabaseId(1);
        }});
        when(mockPersonResponse.hits().hits()).thenReturn(List.of(mockHit));
        when(elasticsearchClient.search(any(Function.class), eq(PersonIndex.class)))
            .thenReturn(mockPersonResponse);

        // When
        var result = service.getResearchersWithMostCitations(institutionId, 2020, 2023);

        // Then
        assertEquals(10, result.size());
        long previous = Long.MAX_VALUE;
        for (var entry : result) {
            assertTrue(entry.b <= previous);
            previous = entry.b;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenNoEligiblePersons() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids"));
            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(institutionId))
                .thenReturn(List.of(123, 456));

            var emptyPersonResponse =
                mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            when(emptyPersonResponse.hits().hits()).thenReturn(List.of());

            when(elasticsearchClient.search(any(Function.class), eq(PersonIndex.class)))
                .thenReturn(emptyPersonResponse);

            // When
            var result =
                service.getResearchersWithMostAssessmentPoints(institutionId, fromYear, toYear);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleEmptyAggregationResults() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids"));
            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(institutionId))
                .thenReturn(List.of(123, 456));
            queryUtilMock.when(() -> QueryUtil.fetchCommissionsForOrganisationUnit(institutionId))
                .thenReturn(Set.of(new Pair<>(5, new HashSet<MultiLingualContent>())));

            var person = new PersonIndex();
            person.setDatabaseId(1);
            when(personIndexRepository.findByDatabaseId(1)).thenReturn(Optional.of(person));

            var personResponse =
                mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            var hit = mock(Hit.class);
            when(hit.source()).thenReturn(person);
            when(personResponse.hits().hits()).thenReturn(List.of(hit));

            var emptyAggResponse = mock(SearchResponse.class);
            when(emptyAggResponse.aggregations()).thenReturn(null);

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(PersonIndex.class)))
                .thenReturn(personResponse);
            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class)))
                .thenReturn(emptyAggResponse);

            // When
            var result =
                service.getResearchersWithMostAssessmentPoints(institutionId, fromYear, toYear);

            // Then
            assertEquals(0, result.size());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenIOExceptionOccurs() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids"));
            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(institutionId))
                .thenReturn(List.of(123, 456));
            queryUtilMock.when(() -> QueryUtil.fetchCommissionsForOrganisationUnit(institutionId))
                .thenReturn(Set.of(new Pair<>(5, new HashSet<MultiLingualContent>())));

            when(elasticsearchClient.search(any(Function.class), any()))
                .thenThrow(new IOException("Elasticsearch error"));

            // When
            var result =
                service.getResearchersWithMostAssessmentPoints(institutionId, fromYear, toYear);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterOutPersonsNotFoundInRepository() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids"));
            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(institutionId))
                .thenReturn(List.of(123, 456));
            queryUtilMock.when(() -> QueryUtil.fetchCommissionsForOrganisationUnit(institutionId))
                .thenReturn(Set.of(new Pair<>(5, new HashSet<MultiLingualContent>())));

            var personIdResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            var personHit = mock(Hit.class);
            var eligiblePerson = new PersonIndex();
            eligiblePerson.setDatabaseId(1);
            when(personHit.source()).thenReturn(eligiblePerson);
            when(personIdResponse.hits().hits()).thenReturn(List.of(personHit));

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(PersonIndex.class)
            )).thenReturn(personIdResponse);

            var bucket1 = mock(LongTermsBucket.class);
            when(bucket1.key()).thenReturn(1L);

            var totalPointsAgg1 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            var sumAgg1 = mock(SumAggregate.class);
            when(sumAgg1.value()).thenReturn(150.5);
            when(totalPointsAgg1.sum()).thenReturn(sumAgg1);
            when(bucket1.aggregations()).thenReturn(Map.of("total_points", totalPointsAgg1));

            var bucket2 = mock(LongTermsBucket.class);

            when(bucket2.key()).thenReturn(2L);

            var totalPointsAgg2 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            var sumAgg2 = mock(SumAggregate.class);
            when(sumAgg2.value()).thenReturn(75.0);
            when(totalPointsAgg2.sum()).thenReturn(sumAgg2);
            when(bucket2.aggregations()).thenReturn(Map.of("total_points", totalPointsAgg2));

            var byPersonAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(byPersonAgg.nested()
                .aggregations().get("filtered_by_person")
                .filter()
                .aggregations().get("person_buckets")
                .lterms().buckets().array()
            ).thenReturn(List.of(bucket1, bucket2));

            var topAggs = Map.of("by_person", byPersonAgg);

            var mockResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            when(mockResponse.aggregations()).thenReturn(topAggs);

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class)))
                .thenReturn(mockResponse);

            var person1 = new PersonIndex();
            person1.setDatabaseId(1);
            person1.setName("Researcher 1");
            when(personIndexRepository.findByDatabaseId(1)).thenReturn(Optional.of(person1));
            when(personIndexRepository.findByDatabaseId(2)).thenReturn(Optional.empty());

            // When
            var result =
                service.getResearchersWithMostAssessmentPoints(institutionId, fromYear, toYear);

            // Then
            assertEquals(1, result.size());
            assertEquals(1, result.getFirst().leaderboardData().size());
            assertEquals(person1, result.getFirst().leaderboardData().getFirst().a);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnTopResearchersByViewCount() throws IOException {
        // Given
        var institutionId = 123;
        var fromDate = LocalDate.of(2023, 1, 1);
        var toDate = LocalDate.of(2023, 12, 31);

        var mockPerson1 = new PersonIndex();
        mockPerson1.setDatabaseId(1);
        var mockPerson2 = new PersonIndex();
        mockPerson2.setDatabaseId(2);
        var mockPerson3 = new PersonIndex();
        mockPerson3.setDatabaseId(3);

        var mockPersonHit1 = mock(Hit.class);
        when(mockPersonHit1.source()).thenReturn(mockPerson1);
        var mockPersonHit2 = mock(Hit.class);
        when(mockPersonHit2.source()).thenReturn(mockPerson2);
        var mockPersonHit3 = mock(Hit.class);
        when(mockPersonHit3.source()).thenReturn(mockPerson3);

        var mockPersonResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockPersonResponse.hits().hits()).thenReturn(
            Arrays.asList(mockPersonHit1, mockPersonHit2, mockPersonHit3));

        var mockBucket1 = mock(LongTermsBucket.class, RETURNS_DEEP_STUBS);
        when(mockBucket1.key()).thenReturn(1L);
        when(mockBucket1.aggregations().get("view_count").valueCount().value()).thenReturn(150.0);

        var mockBucket2 = mock(LongTermsBucket.class, RETURNS_DEEP_STUBS);
        when(mockBucket2.key()).thenReturn(2L);
        when(mockBucket2.aggregations().get("view_count").valueCount().value()).thenReturn(100.0);

        var mockBucket3 = mock(LongTermsBucket.class, RETURNS_DEEP_STUBS);
        when(mockBucket3.key()).thenReturn(3L);
        when(mockBucket3.aggregations().get("view_count").valueCount().value()).thenReturn(
            Double.NaN);

        var buckets = Arrays.asList(mockBucket1, mockBucket2, mockBucket3);
        var mockLongTerms = mock(LongTermsAggregate.class, RETURNS_DEEP_STUBS);
        when(mockLongTerms.buckets().array()).thenReturn(buckets);

        var mockAggregate = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate.lterms()).thenReturn(mockLongTerms);

        var mockAggregations = new HashMap<String, Aggregate>();
        mockAggregations.put("by_person", mockAggregate);

        var mockStatsResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockStatsResponse.aggregations()).thenReturn(mockAggregations);

        var mockDetailedPerson1 = new PersonIndex();
        mockDetailedPerson1.setDatabaseId(1);
        mockDetailedPerson1.setName("John Doe");

        var mockDetailedPerson2 = new PersonIndex();
        mockDetailedPerson2.setDatabaseId(2);
        mockDetailedPerson2.setName("Jane Smith");

        when(personIndexRepository.findByDatabaseId(1)).thenReturn(
            Optional.of(mockDetailedPerson1));
        when(personIndexRepository.findByDatabaseId(2)).thenReturn(
            Optional.of(mockDetailedPerson2));
        when(personIndexRepository.findByDatabaseId(3)).thenReturn(Optional.empty());

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenReturn(mockPersonResponse);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class)
        )).thenReturn(mockStatsResponse);

        // When
        var result = service.getTopResearchersByViewCount(institutionId, fromDate, toDate);

        // Then
        assertEquals(2, result.size());
        assertEquals("John Doe", result.get(0).a.getName());
        assertEquals(150L, result.get(0).b);
        assertEquals("Jane Smith", result.get(1).a.getName());
        assertEquals(100L, result.get(1).b);

        verify(personIndexRepository, times(3)).findByDatabaseId(anyInt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenInstitutionIdIsNull() {
        // Given
        Integer institutionId = null;
        var fromDate = LocalDate.of(2023, 1, 1);
        var toDate = LocalDate.of(2023, 12, 31);

        // When
        var result = service.getTopResearchersByViewCount(institutionId, fromDate, toDate);

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(elasticsearchClient, personIndexRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenNoEligiblePersonIdsFound() throws IOException {
        // Given
        var institutionId = 123;
        var fromDate = LocalDate.of(2023, 1, 1);
        var toDate = LocalDate.of(2023, 12, 31);

        var mockPersonResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockPersonResponse.hits().hits()).thenReturn(Collections.emptyList());

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenReturn(mockPersonResponse);

        // When
        var result = service.getTopResearchersByViewCount(institutionId, fromDate, toDate);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenPersonSearchThrowsIOException() throws IOException {
        // Given
        var institutionId = 123;
        var fromDate = LocalDate.of(2023, 1, 1);
        var toDate = LocalDate.of(2023, 12, 31);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenThrow(new IOException("Connection failed"));

        // When
        var result = service.getTopResearchersByViewCount(institutionId, fromDate, toDate);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenStatisticsSearchThrowsIOException() throws IOException {
        // Given
        var institutionId = 123;
        var fromDate = LocalDate.of(2023, 1, 1);
        var toDate = LocalDate.of(2023, 12, 31);

        var mockPerson = new PersonIndex();
        mockPerson.setDatabaseId(1);
        var mockPersonHit = mock(Hit.class);
        when(mockPersonHit.source()).thenReturn(mockPerson);

        var mockPersonResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockPersonResponse.hits().hits()).thenReturn(List.of(mockPersonHit));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenReturn(mockPersonResponse);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class)
        )).thenThrow(new IOException("Statistics index unavailable"));

        // When
        var result = service.getTopResearchersByViewCount(institutionId, fromDate, toDate);

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(personIndexRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleEmptyAggregationsGracefully() throws IOException {
        // Given
        var institutionId = 123;
        var fromDate = LocalDate.of(2023, 1, 1);
        var toDate = LocalDate.of(2023, 12, 31);

        var mockPerson = new PersonIndex();
        mockPerson.setDatabaseId(1);
        var mockPersonHit = mock(Hit.class);
        when(mockPersonHit.source()).thenReturn(mockPerson);

        var mockPersonResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockPersonResponse.hits().hits()).thenReturn(List.of(mockPersonHit));

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.lterms().buckets().array()).thenReturn(Collections.emptyList());

        var mockStatsResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        var mockAggs = mock(Map.class);
        when(mockStatsResponse.aggregations()).thenReturn(mockAggs);
        when(mockAggs.get("by_person")).thenReturn(mockAgg);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenReturn(mockPersonResponse);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class)
        )).thenReturn(mockStatsResponse);

        // When
        var result = service.getTopResearchersByViewCount(institutionId, fromDate, toDate);

        // Then
        assertTrue(result.isEmpty());
        verify(personIndexRepository, never()).findByDatabaseId(anyInt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldLimitToTop10Researchers() throws IOException {
        // Given
        var institutionId = 123;
        var fromDate = LocalDate.of(2023, 1, 1);
        var toDate = LocalDate.of(2023, 12, 31);

        List<Hit> personHits = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            var mockPerson = new PersonIndex();
            mockPerson.setDatabaseId(i);
            var mockPersonHit = mock(Hit.class);
            when(mockPersonHit.source()).thenReturn(mockPerson);
            personHits.add(mockPersonHit);
        }

        var mockPersonResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockPersonResponse.hits().hits()).thenReturn(personHits);

        List<LongTermsBucket> buckets = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            var mockBucket = mock(LongTermsBucket.class, RETURNS_DEEP_STUBS);
            when(mockBucket.key()).thenReturn((long) i);
            when(mockBucket.aggregations().get("view_count").valueCount().value()).thenReturn(
                (double) (100 - i));
            buckets.add(mockBucket);

            var mockPerson = new PersonIndex();
            mockPerson.setDatabaseId(i);
            mockPerson.setName("Researcher " + i);
            when(personIndexRepository.findByDatabaseId(i)).thenReturn(Optional.of(mockPerson));
        }

        var mockLongTerms = mock(LongTermsAggregate.class, RETURNS_DEEP_STUBS);
        when(mockLongTerms.buckets().array()).thenReturn(buckets);

        var mockAggregate = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate.lterms()).thenReturn(mockLongTerms);

        var mockAggregations = new HashMap<String, Aggregate>();
        mockAggregations.put("by_person", mockAggregate);

        var mockStatsResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockStatsResponse.aggregations()).thenReturn(mockAggregations);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)
        )).thenReturn(mockPersonResponse);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class)
        )).thenReturn(mockStatsResponse);

        // When
        var result = service.getTopResearchersByViewCount(institutionId, fromDate, toDate);

        // Then
        assertEquals(10, result.size());
        assertTrue(result.get(0).b >= result.get(1).b);
        assertTrue(result.get(1).b >= result.get(2).b);
    }
}
