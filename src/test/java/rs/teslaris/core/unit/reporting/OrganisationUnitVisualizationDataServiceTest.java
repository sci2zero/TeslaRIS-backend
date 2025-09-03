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
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MinAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.institution.OrganisationUnitOutputConfigurationDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.reporting.service.impl.OrganisationUnitVisualizationDataServiceImpl;

@SpringBootTest
class OrganisationUnitVisualizationDataServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private OrganisationUnitOutputConfigurationService organisationUnitOutputConfigurationService;

    @Mock
    private UserService userService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private OrganisationUnitVisualizationDataServiceImpl service;


    @Test
    void shouldReturnPublicationCountsForOrganisationUnit() throws IOException {
        // Given
        var organisationUnitId = 123;
        var startYear = 2020;
        var endYear = 2022;
        var searchFields = List.of("field1", "field2");

        when(organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(mockOutputConfiguration(true, true, true));

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
        assertEquals(2020, result.get(0).year());
        assertEquals(1, result.get(0).countsByCategory().size());
        assertEquals(5L, result.get(0).countsByCategory().get("ARTICLE"));
    }

    @Test
    void shouldReturnEmptyListWhenNoYearRangeForPublicationCounts() throws IOException {
        // Given
        var organisationUnitId = 123;
        Integer startYear = null;
        Integer endYear = null;

        when(organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(mockOutputConfiguration(true, true, true));

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
    void shouldReturnMCategoriesForOrganisationUnitPublications() throws IOException {
        // Given
        var organisationUnitId = 123;
        var startYear = 2020;
        var endYear = 2022;

        when(organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(mockOutputConfiguration(true, true, true));

        when(userService.findCommissionForOrganisationUnitId(organisationUnitId)).thenReturn(
            List.of(
                new Commission() {{
                    setId(1);
                    setDescription(
                        Set.of(new MultiLingualContent(new LanguageTag(), "Commission 1", 1)));
                }}
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

    @Test
    void shouldReturnEmptyListWhenIOExceptionInMCategories() throws IOException {
        // Given
        var organisationUnitId = 123;
        var startYear = 2020;
        var endYear = 2022;

        when(organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(mockOutputConfiguration(true, true, true));

        when(userService.findCommissionForOrganisationUnitId(organisationUnitId)).thenReturn(
            List.of(
                new Commission() {{
                    setId(1);
                    setDescription(
                        Set.of(new MultiLingualContent(new LanguageTag(), "Commission 1", 1)));
                }}
            ));

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
    void shouldReturnMCategoryCountsForOrganisationUnit() throws IOException {
        // Given
        var organisationUnitId = 123;
        var startYear = 2020;
        var endYear = 2022;
        var searchFields = List.of("field1", "field2");

        when(organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(mockOutputConfiguration(true, true, true));

        // Mock year range
        var mockMinAgg = mock(MinAggregate.class);
        when(mockMinAgg.value()).thenReturn(2020.0);
        var mockMaxAgg = mock(MaxAggregate.class);
        when(mockMaxAgg.value()).thenReturn(2022.0);

        var mockRangeAggregations = mock(HashMap.class);
        when(mockRangeAggregations.get("earliestYear")).thenReturn(mockMinAgg);
        when(mockRangeAggregations.get("latestYear")).thenReturn(mockMaxAgg);

        var mockRangeResponse = mock(SearchResponse.class);
        when(mockRangeResponse.aggregations()).thenReturn(mockRangeAggregations);

        when(userService.findCommissionForOrganisationUnitId(organisationUnitId)).thenReturn(
            List.of(
                new Commission() {{
                    setId(1);
                    setDescription(
                        Set.of(new MultiLingualContent(new LanguageTag(), "Commission 1", 1)));
                }}
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
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenReturn(mockRangeResponse) // For year range
            .thenReturn(mockYearlyResponse) // For 2020
            .thenReturn(mockYearlyResponse) // For 2021
            .thenReturn(mockYearlyResponse); // For 2022

        // When
        var result =
            service.getMCategoryCountsForOrganisationUnit(organisationUnitId, startYear, endYear);

        // Then
        assertEquals(1, result.size());
        var commissionCounts = result.getFirst();
        assertEquals(3, commissionCounts.yearlyCounts().size());
        assertEquals(2020, commissionCounts.yearlyCounts().get(0).year());
        assertEquals(1, commissionCounts.yearlyCounts().get(0).countsByCategory().size());
        assertEquals(2L, commissionCounts.yearlyCounts().get(0).countsByCategory().get("M11"));
    }

    @Test
    void shouldReturnCountryStatisticsForOrganisationUnit() throws IOException {
        // Given
        var organisationUnitId = 123;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 12, 31);

        when(organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(mockOutputConfiguration(true, true, true));

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setId(organisationUnitId);
            }}
        );

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

        when(elasticsearchClient.search(any(Function.class), eq(Void.class))).thenReturn(
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

    @Test
    void shouldReturnEmptyListWhenIOExceptionInCountryStatistics() throws IOException {
        // Given
        var organisationUnitId = 123;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 12, 31);

        when(organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(mockOutputConfiguration(true, true, true));

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setId(organisationUnitId);
            }}
        );

        when(elasticsearchClient.search(any(Function.class), eq(Void.class))).thenThrow(
            new IOException());

        // When
        var result =
            service.getByCountryStatisticsForOrganisationUnit(organisationUnitId, from, to);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnMonthlyStatisticsCounts() throws IOException {
        // Given
        var organisationUnitId = 123;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 3, 31);

        when(organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(mockOutputConfiguration(true, true, true));

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setId(organisationUnitId);
            }}
        );

        // Mock ES response
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

        when(elasticsearchClient.search(any(Function.class), eq(Void.class))).thenReturn(
            mockResponse);

        // When
        var result = service.getMonthlyStatisticsCounts(organisationUnitId, from, to);

        // Then
        assertEquals(3, result.size());
        assertEquals(15L, result.get(YearMonth.of(2023, 1)));
        assertEquals(20L, result.get(YearMonth.of(2023, 2)));
        assertEquals(25L, result.get(YearMonth.of(2023, 3)));
    }

    @Test
    void shouldReturnEmptyMapWhenIOExceptionInMonthlyStatistics() throws IOException {
        // Given
        var organisationUnitId = 123;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 3, 31);

        when(organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(mockOutputConfiguration(true, true, true));

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setId(organisationUnitId);
            }}
        );

        when(elasticsearchClient.search(any(Function.class), eq(Void.class))).thenThrow(
            new IOException());

        // When
        var result = service.getMonthlyStatisticsCounts(organisationUnitId, from, to);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnYearlyStatisticsCounts() throws IOException {
        // Given
        var organisationUnitId = 123;
        var startYear = 2022;
        var endYear = 2023;

        when(organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(mockOutputConfiguration(true, true, true));

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setId(organisationUnitId);
            }}
        );

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

        when(elasticsearchClient.search(any(Function.class), eq(Void.class))).thenReturn(
            mockResponse);

        // When
        var result = service.getYearlyStatisticsCounts(organisationUnitId, startYear, endYear);

        // Then
        assertEquals(2, result.size());
        assertEquals(30L, result.get(Year.of(2022)));
        assertEquals(45L, result.get(Year.of(2023)));
    }

    @Test
    void shouldReturnEmptyMapWhenIOExceptionInYearlyStatistics() throws IOException {
        // Given
        var organisationUnitId = 123;
        var startYear = 2022;
        var endYear = 2023;

        when(organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(mockOutputConfiguration(true, true, true));

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setId(organisationUnitId);
            }}
        );

        when(elasticsearchClient.search(any(Function.class), eq(Void.class))).thenThrow(
            new IOException());

        // When
        var result = service.getYearlyStatisticsCounts(organisationUnitId, startYear, endYear);

        // Then
        assertTrue(result.isEmpty());
    }

    private OrganisationUnitOutputConfigurationDTO mockOutputConfiguration(boolean showSpecified,
                                                                           boolean showPublicationYear,
                                                                           boolean showCurrent) {
        return new OrganisationUnitOutputConfigurationDTO(true, showSpecified,
            showPublicationYear, showCurrent);
    }
}
