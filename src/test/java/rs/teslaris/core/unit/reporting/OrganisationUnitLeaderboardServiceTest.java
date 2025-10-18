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
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.impl.leaderboards.OrganisationUnitLeaderboardServiceImpl;
import rs.teslaris.reporting.utility.QueryUtil;

@SpringBootTest
public class OrganisationUnitLeaderboardServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private OrganisationUnitIndexRepository organisationUnitIndexRepository;

    @InjectMocks
    private OrganisationUnitLeaderboardServiceImpl organisationUnitLeaderboardService;

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnTopSubUnitsByPublicationCount() throws IOException {
        try (var queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids"));

            var eligibleOUIds = List.of(1, 2, 3);
            var ouResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            var hit = mock(Hit.class);
            var ouIndex = new OrganisationUnitIndex();
            ouIndex.setDatabaseId(1);
            when(hit.source()).thenReturn(ouIndex);
            when(ouResponse.hits().hits()).thenReturn(List.of(hit));
            when(elasticsearchClient.search(any(Function.class), eq(OrganisationUnitIndex.class)))
                .thenReturn(ouResponse);

            var bucket1 = mock(LongTermsBucket.class);
            when(bucket1.key()).thenReturn(1L);
            when(bucket1.docCount()).thenReturn(10L);
            var bucket2 = mock(LongTermsBucket.class);
            when(bucket2.key()).thenReturn(2L);
            when(bucket2.docCount()).thenReturn(5L);

            var termsAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(termsAgg.lterms().buckets().array()).thenReturn(List.of(bucket1, bucket2));

            var mockResponse = mock(SearchResponse.class);
            when(mockResponse.aggregations()).thenReturn(Map.of("by_org_unit", termsAgg));

            when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
                .thenReturn(mockResponse);

            var ou1 = new OrganisationUnitIndex();
            ou1.setDatabaseId(1);
            var ou2 = new OrganisationUnitIndex();
            ou2.setDatabaseId(2);
            when(organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(1))
                .thenReturn(Optional.of(ou1));
            when(organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(2))
                .thenReturn(Optional.of(ou2));

            // When
            var result = organisationUnitLeaderboardService.getTopSubUnitsByPublicationCount(
                institutionId, fromYear, toYear);

            // Then
            assertEquals(2, result.size());
            assertEquals(10L, result.get(0).b);
            assertEquals(5L, result.get(1).b);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnSubUnitsWithMostCitations() throws IOException {
        try (var queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2021;

            var ouResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            var hit = mock(Hit.class);
            var ouIndex = new OrganisationUnitIndex();
            ouIndex.setDatabaseId(1);
            when(hit.source()).thenReturn(ouIndex);
            when(ouResponse.hits().hits()).thenReturn(List.of(hit));
            when(elasticsearchClient.search(any(Function.class), eq(OrganisationUnitIndex.class)))
                .thenReturn(ouResponse);

            var bucket1 = mock(LongTermsBucket.class);
            when(bucket1.key()).thenReturn(1L);
            var sum2020a = mock(SumAggregate.class);
            when(sum2020a.value()).thenReturn(40.0);
            var sum2021a = mock(SumAggregate.class);
            when(sum2021a.value()).thenReturn(60.0);
            when(bucket1.aggregations()).thenReturn(Map.of(
                "year_2020", mock(Aggregate.class, RETURNS_DEEP_STUBS),
                "year_2021", mock(Aggregate.class, RETURNS_DEEP_STUBS)
            ));
            when(bucket1.aggregations().get("year_2020").sum()).thenReturn(sum2020a);
            when(bucket1.aggregations().get("year_2021").sum()).thenReturn(sum2021a);

            var bucket2 = mock(LongTermsBucket.class);
            when(bucket2.key()).thenReturn(2L);
            var sum2020b = mock(SumAggregate.class);
            when(sum2020b.value()).thenReturn(30.0);
            var sum2021b = mock(SumAggregate.class);
            when(sum2021b.value()).thenReturn(20.0);
            when(bucket2.aggregations()).thenReturn(Map.of(
                "year_2020", mock(Aggregate.class, RETURNS_DEEP_STUBS),
                "year_2021", mock(Aggregate.class, RETURNS_DEEP_STUBS)
            ));
            when(bucket2.aggregations().get("year_2020").sum()).thenReturn(sum2020b);
            when(bucket2.aggregations().get("year_2021").sum()).thenReturn(sum2021b);

            var termsAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(termsAgg.lterms().buckets().array()).thenReturn(List.of(bucket1, bucket2));

            var mockResponse = mock(SearchResponse.class);
            when(mockResponse.aggregations()).thenReturn(Map.of("by_org_unit", termsAgg));
            when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
                .thenReturn(mockResponse);

            var ou1 = new OrganisationUnitIndex();
            ou1.setDatabaseId(1);
            var ou2 = new OrganisationUnitIndex();
            ou2.setDatabaseId(2);
            when(organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(1))
                .thenReturn(Optional.of(ou1));
            when(organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(2))
                .thenReturn(Optional.of(ou2));

            // When
            var result = organisationUnitLeaderboardService.getSubUnitsWithMostCitations(
                institutionId, fromYear, toYear);

            // Then
            assertEquals(2, result.size());
            assertEquals(100L, result.get(0).b); // 40 + 60
            assertEquals(50L, result.get(1).b);  // 30 + 20
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnSubUnitsWithMostAssessmentPoints() throws IOException {
        try (var queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids"));
            queryUtilMock.when(() -> QueryUtil.fetchCommissionsForOrganisationUnit(institutionId))
                .thenReturn(Set.of(new Pair<>(5, new HashSet<MultiLingualContent>())));

            var ouResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            var hit = mock(Hit.class);
            var ouIndex = new OrganisationUnitIndex();
            ouIndex.setDatabaseId(1);
            when(hit.source()).thenReturn(ouIndex);
            when(ouResponse.hits().hits()).thenReturn(List.of(hit));
            when(elasticsearchClient.search(any(Function.class), eq(OrganisationUnitIndex.class)))
                .thenReturn(ouResponse);

            var bucket1 = mock(LongTermsBucket.class);
            when(bucket1.key()).thenReturn(1L);
            var sumAgg1 = mock(SumAggregate.class);
            when(sumAgg1.value()).thenReturn(150.5);
            var agg1 = mock(Aggregate.class);
            when(agg1.sum()).thenReturn(sumAgg1);
            when(bucket1.aggregations()).thenReturn(Map.of("total_points", agg1));

            var termsAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(termsAgg.lterms().buckets().array()).thenReturn(List.of(bucket1));

            var mockResponse = mock(SearchResponse.class);
            when(mockResponse.aggregations()).thenReturn(Map.of("by_org_unit", termsAgg));

            when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
                .thenReturn(mockResponse);

            var ou1 = new OrganisationUnitIndex();
            ou1.setDatabaseId(1);
            when(organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(1))
                .thenReturn(Optional.of(ou1));

            // When
            var result = organisationUnitLeaderboardService.getSubUnitsWithMostAssessmentPoints(
                institutionId, fromYear, toYear);

            // Then
            assertEquals(1, result.size());
            assertEquals(5, result.get(0).commissionId());
            assertEquals(1, result.get(0).leaderboardData().size());
            assertEquals(150.5, result.get(0).leaderboardData().get(0).b);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenNoEligibleOUs() throws IOException {
        try (var queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids"));

            var emptyResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            when(emptyResponse.hits().hits()).thenReturn(List.of());
            when(elasticsearchClient.search(any(Function.class), eq(OrganisationUnitIndex.class)))
                .thenReturn(emptyResponse);

            // When
            var result = organisationUnitLeaderboardService.getTopSubUnitsByPublicationCount(
                institutionId, fromYear, toYear);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenIOException() throws IOException {
        try (var queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids"));

            when(elasticsearchClient.search(any(Function.class), any()))
                .thenThrow(new IOException("Connection failed"));

            // When
            var result = organisationUnitLeaderboardService.getTopSubUnitsByPublicationCount(
                institutionId, fromYear, toYear);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleEmptyAggregationResults() throws IOException {
        try (var queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var institutionId = 123;
            var fromYear = 2020;
            var toYear = 2023;

            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("organisation_unit_ids"));

            var ouResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            var hit = mock(Hit.class);
            var ouIndex = new OrganisationUnitIndex();
            ouIndex.setDatabaseId(1);
            when(hit.source()).thenReturn(ouIndex);
            when(ouResponse.hits().hits()).thenReturn(List.of(hit));
            when(elasticsearchClient.search(any(Function.class), eq(OrganisationUnitIndex.class)))
                .thenReturn(ouResponse);

            var termsAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(termsAgg.lterms().buckets().array()).thenReturn(List.of());

            var mockResponse = mock(SearchResponse.class);
            when(mockResponse.aggregations()).thenReturn(Map.of("by_org_unit", termsAgg));

            when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
                .thenReturn(mockResponse);

            // When
            var result = organisationUnitLeaderboardService.getTopSubUnitsByPublicationCount(
                institutionId, fromYear, toYear);

            // Then
            assertTrue(result.isEmpty());
        }
    }
}
