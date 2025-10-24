package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MinAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.institution.OrganisationUnitOutputConfigurationDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.impl.visualizations.OrganisationUnitVisualizationDataServiceImpl;
import rs.teslaris.reporting.utility.QueryUtil;

@SpringBootTest
class OrganisationUnitVisualizationDataServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private OrganisationUnitVisualizationDataServiceImpl service;


    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnPublicationCountsForOrganisationUnit() throws IOException {
        // Given
        var organisationUnitId = 123;
        var startYear = 2020;
        var endYear = 2022;

        // Mock year range response
        var mockMinAgg = mock(MinAggregate.class);
        when(mockMinAgg.value()).thenReturn(2020.0);
        var mockMaxAgg = mock(MaxAggregate.class);
        when(mockMaxAgg.value()).thenReturn(2022.0);

        var mockAggregations = mock(HashMap.class);
        when(mockAggregations.get("earliestYear")).thenReturn(mockMinAgg);
        when(mockAggregations.get("latestYear")).thenReturn(mockMaxAgg);

        var mockRangeResponse = mock(SearchResponse.class);
        when(mockRangeResponse.aggregations()).thenReturn(mockAggregations);

        // Mock yearly count responses
        var mockBucket = mock(StringTermsBucket.class);
        when(mockBucket.key()).thenReturn(FieldValue.of("ARTICLE"));
        when(mockBucket.docCount()).thenReturn(5L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.sterms().buckets().array()).thenReturn(List.of(mockBucket));
        when(mockAggregations.get("by_type")).thenReturn(mockAgg);

        var mockYearlyAggregations = mock(HashMap.class);
        when(mockYearlyAggregations.get("by_type")).thenReturn(mockAgg);

        var mockYearlyResponse = mock(SearchResponse.class);
        when(mockYearlyResponse.aggregations()).thenReturn(mockYearlyAggregations);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenReturn(mockRangeResponse) // For year range
            .thenReturn(mockYearlyResponse) // For 2020
            .thenReturn(mockYearlyResponse) // For 2021
            .thenReturn(mockYearlyResponse); // For 2022

        // When
        var result =
            service.getPublicationCountsForOrganisationUnit(organisationUnitId, startYear, endYear);

        // Then
        assertEquals(3, result.size());
        assertEquals(2020, result.getFirst().year());
        assertEquals(1, result.getFirst().countsByCategory().size());
        assertEquals(5L, result.getFirst().countsByCategory().get("ARTICLE"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenNoYearRangeForPublicationCounts() throws IOException {
        // Given
        var organisationUnitId = 123;
        Integer startYear = null;
        Integer endYear = null;

        // Mock empty year range response
        var mockMinAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockMinAgg.min().value()).thenReturn(Double.NaN);
        var mockMaxAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockMaxAgg.min().value()).thenReturn(Double.NaN);
        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);

        var mockAggregations = mock(HashMap.class);
        when(mockAggregations.get("earliestYear")).thenReturn(mockMinAgg);
        when(mockAggregations.get("latestYear")).thenReturn(mockMaxAgg);
        when(mockAggregations.get("by_type")).thenReturn(mockAgg);

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(mockAggregations);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenReturn(mockResponse);

        // When
        var result =
            service.getPublicationCountsForOrganisationUnit(organisationUnitId, startYear, endYear);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnMCategoriesForOrganisationUnitPublications() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var organisationUnitId = 123;
            var startYear = 2020;
            var endYear = 2022;

            queryUtilMock.when(
                    () -> QueryUtil.fetchCommissionsForOrganisationUnit(organisationUnitId))
                .thenReturn(Set.of(new Pair<>(
                    1, Set.of(new MultiLingualContent(new LanguageTag(), "Commission 1", 1)))
                ));

            var mockBucket = mock(StringTermsBucket.class);
            when(mockBucket.key()).thenReturn(FieldValue.of("M21"));
            when(mockBucket.docCount()).thenReturn(3L);

            var mockTermsAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(mockTermsAgg.sterms().buckets()).thenReturn(mock(Buckets.class));
            when(mockTermsAgg.sterms().buckets().array()).thenReturn(List.of(mockBucket));

            var mockAggregations = mock(HashMap.class);
            when(mockAggregations.get("by_m_category")).thenReturn(mockTermsAgg);

            var mockResponse = mock(SearchResponse.class);
            when(mockResponse.aggregations()).thenReturn(mockAggregations);

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class))).thenReturn(
                mockResponse);

            // When
            var result =
                service.getOrganisationUnitPublicationsByMCategories(organisationUnitId, startYear,
                    endYear);

            // Then
            assertEquals(1, result.size());
            var mCategoryCounts = result.getFirst();
            assertEquals(1, mCategoryCounts.countsByCategory().size());
            assertEquals(3L, mCategoryCounts.countsByCategory().get("M21"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenIOExceptionInMCategories() throws IOException {
        // Given
        var organisationUnitId = 123;
        var startYear = 2020;
        var endYear = 2022;

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenThrow(
            new IOException());

        // When
        var result =
            service.getOrganisationUnitPublicationsByMCategories(organisationUnitId, startYear,
                endYear);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnMCategoryCountsForOrganisationUnit() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var organisationUnitId = 123;
            var startYear = 2020;
            var endYear = 2022;

            var mockMinAgg = mock(MinAggregate.class);
            when(mockMinAgg.value()).thenReturn(2020.0);
            var mockMaxAgg = mock(MaxAggregate.class);
            when(mockMaxAgg.value()).thenReturn(2022.0);

            var mockRangeAggregations = mock(HashMap.class);
            when(mockRangeAggregations.get("earliestYear")).thenReturn(mockMinAgg);
            when(mockRangeAggregations.get("latestYear")).thenReturn(mockMaxAgg);

            var mockRangeResponse = mock(SearchResponse.class);
            when(mockRangeResponse.aggregations()).thenReturn(mockRangeAggregations);

            queryUtilMock.when(
                    () -> QueryUtil.fetchCommissionsForOrganisationUnit(organisationUnitId))
                .thenReturn(Set.of(new Pair<>(
                    1, Set.of(new MultiLingualContent(new LanguageTag(), "Commission 1", 1)))
                ));

            var mockBucket = mock(StringTermsBucket.class);
            when(mockBucket.key()).thenReturn(FieldValue.of("M11"));
            when(mockBucket.docCount()).thenReturn(2L);

            var mockTermsAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(mockTermsAgg.sterms().buckets().array()).thenReturn(List.of(mockBucket));

            var mockYearlyAggregations = mock(HashMap.class);
            when(mockYearlyAggregations.get("by_m_category")).thenReturn(mockTermsAgg);
            when(mockRangeAggregations.get("by_m_category")).thenReturn(mockTermsAgg);

            var mockYearlyResponse = mock(SearchResponse.class);
            when(mockYearlyResponse.aggregations()).thenReturn(mockYearlyAggregations);

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class)))
                .thenReturn(mockRangeResponse) // For year range
                .thenReturn(mockYearlyResponse) // For 2020
                .thenReturn(mockYearlyResponse) // For 2021
                .thenReturn(mockYearlyResponse); // For 2022

            // When
            var result =
                service.getMCategoryCountsForOrganisationUnit(organisationUnitId, startYear,
                    endYear);

            // Then
            assertEquals(1, result.size());
            var commissionCounts = result.getFirst();
            assertEquals(3, commissionCounts.yearlyCounts().size());
            assertEquals(2020, commissionCounts.yearlyCounts().getFirst().year());
            assertEquals(1, commissionCounts.yearlyCounts().getFirst().countsByCategory().size());
            assertEquals(2L,
                commissionCounts.yearlyCounts().getFirst().countsByCategory().get("M11"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnCountryStatisticsForOrganisationUnit() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var organisationUnitId = 123;
            var from = LocalDate.of(2023, 1, 1);
            var to = LocalDate.of(2023, 12, 31);

            queryUtilMock.when(
                    () -> QueryUtil.getOrganisationUnitOutputSearchFields(organisationUnitId))
                .thenReturn(List.of("organisation_unit_ids", "organisation_unit_ids_active"));

            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(organisationUnitId))
                .thenReturn(List.of(123, 456, 789));

            var mockCountryBucket = mock(StringTermsBucket.class);
            when(mockCountryBucket.key()).thenReturn(FieldValue.of("US"));
            when(mockCountryBucket.docCount()).thenReturn(10L);

            var mockNameBucket = mock(StringTermsBucket.class);
            when(mockNameBucket.key()).thenReturn(FieldValue.of("United States"));

            var mockNameAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(mockNameAgg.sterms().buckets().array()).thenReturn(List.of(mockNameBucket));

            var mockSubAggregations = mock(HashMap.class);
            when(mockSubAggregations.get("country_name")).thenReturn(mockNameAgg);

            when(mockCountryBucket.aggregations()).thenReturn(mockSubAggregations);

            var mockTermsAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(mockTermsAgg.sterms().buckets().array()).thenReturn(List.of(mockCountryBucket));

            var mockAggregations = mock(HashMap.class);
            when(mockAggregations.get("by_country")).thenReturn(mockTermsAgg);

            var mockResponse = mock(SearchResponse.class);
            when(mockResponse.aggregations()).thenReturn(mockAggregations);

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class))).thenReturn(
                mockResponse);

            // When
            var result =
                service.getByCountryStatisticsForOrganisationUnit(organisationUnitId, from, to);

            // Then
            assertEquals(1, result.size());
            var stats = result.getFirst();
            assertEquals("US", stats.countryCode());
            assertEquals("United States", stats.countryName());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenIOExceptionInCountryStatistics() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var organisationUnitId = 123;
            var from = LocalDate.of(2023, 1, 1);
            var to = LocalDate.of(2023, 12, 31);

            queryUtilMock.when(
                    () -> QueryUtil.getOrganisationUnitOutputSearchFields(organisationUnitId))
                .thenReturn(List.of("organisation_unit_ids", "organisation_unit_ids_active"));

            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(organisationUnitId))
                .thenReturn(List.of(123, 456, 789));

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class))).thenThrow(
                new IOException());

            // When
            var result =
                service.getByCountryStatisticsForOrganisationUnit(organisationUnitId, from, to);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnMonthlyStatisticsCounts() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var organisationUnitId = 123;
            var from = LocalDate.of(2023, 1, 1);
            var to = LocalDate.of(2023, 3, 31);

            queryUtilMock.when(
                    () -> QueryUtil.getOrganisationUnitOutputSearchFields(organisationUnitId))
                .thenReturn(List.of("organisation_unit_ids", "organisation_unit_ids_active"));

            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(organisationUnitId))
                .thenReturn(List.of(123, 456, 789));

            var mockBucket1 = mock(DateHistogramBucket.class);
            when(mockBucket1.keyAsString()).thenReturn("2023-01");
            when(mockBucket1.docCount()).thenReturn(15L);

            var mockBucket2 = mock(DateHistogramBucket.class);
            when(mockBucket2.keyAsString()).thenReturn("2023-02");
            when(mockBucket2.docCount()).thenReturn(20L);

            var mockBucket3 = mock(DateHistogramBucket.class);
            when(mockBucket3.keyAsString()).thenReturn("2023-03");
            when(mockBucket3.docCount()).thenReturn(25L);

            var mockHistogramAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(mockHistogramAgg.dateHistogram().buckets().array()).thenReturn(
                List.of(mockBucket1, mockBucket2, mockBucket3));

            var mockAggregations = mock(HashMap.class);
            when(mockAggregations.get("per_month")).thenReturn(mockHistogramAgg);

            var mockResponse = mock(SearchResponse.class);
            when(mockResponse.aggregations()).thenReturn(mockAggregations);

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class))).thenReturn(
                mockResponse);

            // When
            var result = service.getMonthlyStatisticsCounts(organisationUnitId, from, to);

            // Then
            assertEquals(3, result.size());
            assertEquals(15L, result.get(YearMonth.of(2023, 1)));
            assertEquals(20L, result.get(YearMonth.of(2023, 2)));
            assertEquals(25L, result.get(YearMonth.of(2023, 3)));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenIOExceptionInMonthlyStatistics() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var organisationUnitId = 123;
            var from = LocalDate.of(2023, 1, 1);
            var to = LocalDate.of(2023, 3, 31);

            queryUtilMock.when(
                    () -> QueryUtil.getOrganisationUnitOutputSearchFields(organisationUnitId))
                .thenReturn(List.of("organisation_unit_ids", "organisation_unit_ids_active"));

            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(organisationUnitId))
                .thenReturn(List.of(123, 456, 789));

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class))).thenThrow(
                new IOException());

            // When
            var result = service.getMonthlyStatisticsCounts(organisationUnitId, from, to);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnYearlyStatisticsCounts() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var organisationUnitId = 123;
            var startYear = 2022;
            var endYear = 2023;

            queryUtilMock.when(
                    () -> QueryUtil.getOrganisationUnitOutputSearchFields(organisationUnitId))
                .thenReturn(List.of("organisation_unit_ids", "organisation_unit_ids_active"));

            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(organisationUnitId))
                .thenReturn(List.of(123, 456, 789));

            // Mock ES response
            var mockBucket1 = mock(DateHistogramBucket.class);
            when(mockBucket1.keyAsString()).thenReturn("2022");
            when(mockBucket1.docCount()).thenReturn(30L);

            var mockBucket2 = mock(DateHistogramBucket.class);
            when(mockBucket2.keyAsString()).thenReturn("2023");
            when(mockBucket2.docCount()).thenReturn(45L);

            var mockHistogramAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(mockHistogramAgg.dateHistogram().buckets().array()).thenReturn(
                List.of(mockBucket1, mockBucket2));

            var mockAggregations = mock(HashMap.class);
            when(mockAggregations.get("per_year")).thenReturn(mockHistogramAgg);

            var mockResponse = mock(SearchResponse.class);
            when(mockResponse.aggregations()).thenReturn(mockAggregations);

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class))).thenReturn(mockResponse);

            // When
            var result = service.getYearlyStatisticsCounts(organisationUnitId, startYear, endYear);

            // Then
            assertEquals(2, result.size());
            assertEquals(30L, result.get(Year.of(2022)));
            assertEquals(45L, result.get(Year.of(2023)));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenIOExceptionInYearlyStatistics() throws IOException {
        try (MockedStatic<QueryUtil> queryUtilMock = mockStatic(QueryUtil.class)) {
            // Given
            var organisationUnitId = 123;
            var startYear = 2022;
            var endYear = 2023;

            queryUtilMock.when(
                    () -> QueryUtil.getOrganisationUnitOutputSearchFields(organisationUnitId))
                .thenReturn(List.of("organisation_unit_ids", "organisation_unit_ids_active"));

            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(organisationUnitId))
                .thenReturn(List.of(123, 456, 789));

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class))).thenThrow(new IOException());

            // When
            var result = service.getYearlyStatisticsCounts(organisationUnitId, startYear, endYear);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnCitationsByYearForInstitution() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        var toYear = 2023;

        var eligibleOUIds = List.of(123, 456, 789);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(eligibleOUIds);

        var mockAggregate2020 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate2020.sum()).thenReturn(mock(SumAggregate.class));
        when(mockAggregate2020.sum().value()).thenReturn(150.0);

        var mockAggregate2021 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate2021.sum()).thenReturn(mock(SumAggregate.class));
        when(mockAggregate2021.sum().value()).thenReturn(200.0);

        var mockAggregate2022 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate2022.sum()).thenReturn(mock(SumAggregate.class));
        when(mockAggregate2022.sum().value()).thenReturn(180.0);

        var mockAggregate2023 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate2023.sum()).thenReturn(mock(SumAggregate.class));
        when(mockAggregate2023.sum().value()).thenReturn(Double.NaN);

        var mockAggregations = new HashMap<String, Aggregate>();
        mockAggregations.put("year_2020", mockAggregate2020);
        mockAggregations.put("year_2021", mockAggregate2021);
        mockAggregations.put("year_2022", mockAggregate2022);
        mockAggregations.put("year_2023", mockAggregate2023);

        var mockResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockResponse.aggregations()).thenReturn(mockAggregations);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class)
        )).thenReturn(mockResponse);

        // When
        var result = service.getCitationsByYearForInstitution(institutionId, fromYear, toYear);

        // Then
        assertEquals(4, result.size());
        assertEquals(150L, result.get(Year.of(2020)));
        assertEquals(200L, result.get(Year.of(2021)));
        assertEquals(180L, result.get(Year.of(2022)));
        assertEquals(0L, result.get(Year.of(2023)));
        var years = new ArrayList<>(result.keySet());
        assertEquals(Year.of(2020), years.get(0));
        assertEquals(Year.of(2021), years.get(1));
        assertEquals(Year.of(2022), years.get(2));
        assertEquals(Year.of(2023), years.get(3));

        verify(organisationUnitService).getOrganisationUnitIdsFromSubHierarchy(institutionId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenInstitutionIdIsNull() {
        // Given
        Integer institutionId = null;
        var fromYear = 2020;
        var toYear = 2023;

        // When
        var result = service.getCitationsByYearForInstitution(institutionId, fromYear, toYear);

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(organisationUnitService, elasticsearchClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenFromYearIsNull() {
        // Given
        var institutionId = 123;
        Integer fromYear = null;
        var toYear = 2023;

        // When
        var result = service.getCitationsByYearForInstitution(institutionId, fromYear, toYear);

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(organisationUnitService, elasticsearchClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenToYearIsNull() {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        Integer toYear = null;

        // When
        var result = service.getCitationsByYearForInstitution(institutionId, fromYear, toYear);

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(organisationUnitService, elasticsearchClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenElasticsearchThrowsIOException() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        var toYear = 2023;

        var eligibleOUIds = List.of(123, 456);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(eligibleOUIds);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class)
        )).thenThrow(new IOException("Connection failed"));

        // When
        var result = service.getCitationsByYearForInstitution(institutionId, fromYear, toYear);

        // Then
        assertTrue(result.isEmpty());
        verify(organisationUnitService).getOrganisationUnitIdsFromSubHierarchy(institutionId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleSingleYearRange() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2023;
        var toYear = 2023; // Single year

        var eligibleOUIds = List.of(123, 456);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(eligibleOUIds);

        var mockAggregate2023 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate2023.sum()).thenReturn(mock(SumAggregate.class));
        when(mockAggregate2023.sum().value()).thenReturn(300.0);

        var mockAggregations = new HashMap<String, Aggregate>();
        mockAggregations.put("year_2023", mockAggregate2023);

        var mockResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockResponse.aggregations()).thenReturn(mockAggregations);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class)
        )).thenReturn(mockResponse);

        // When
        var result = service.getCitationsByYearForInstitution(institutionId, fromYear, toYear);

        // Then
        assertEquals(1, result.size());
        assertEquals(300L, result.get(Year.of(2023)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleZeroCitationsForAllYears() throws IOException {
        // Given
        var institutionId = 123;
        var fromYear = 2020;
        var toYear = 2022;

        var eligibleOUIds = List.of(123, 456);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(eligibleOUIds);

        var mockAggregate2020 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate2020.sum()).thenReturn(mock(SumAggregate.class));
        when(mockAggregate2020.sum().value()).thenReturn(0.0);

        var mockAggregate2021 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate2021.sum()).thenReturn(mock(SumAggregate.class));
        when(mockAggregate2021.sum().value()).thenReturn(0.0);

        var mockAggregate2022 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate2022.sum()).thenReturn(mock(SumAggregate.class));
        when(mockAggregate2022.sum().value()).thenReturn(0.0);

        var mockAggregations = new HashMap<String, Aggregate>();
        mockAggregations.put("year_2020", mockAggregate2020);
        mockAggregations.put("year_2021", mockAggregate2021);
        mockAggregations.put("year_2022", mockAggregate2022);

        var mockResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockResponse.aggregations()).thenReturn(mockAggregations);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class)
        )).thenReturn(mockResponse);

        // When
        var result = service.getCitationsByYearForInstitution(institutionId, fromYear, toYear);

        // Then
        assertEquals(3, result.size());
        assertEquals(0L, result.get(Year.of(2020)));
        assertEquals(0L, result.get(Year.of(2021)));
        assertEquals(0L, result.get(Year.of(2022)));
    }

    private OrganisationUnitOutputConfigurationDTO mockOutputConfiguration() {
        return new OrganisationUnitOutputConfigurationDTO(true, true, true, true);
    }
}
