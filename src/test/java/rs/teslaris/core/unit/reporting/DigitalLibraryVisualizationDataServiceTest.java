package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MinAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.reporting.service.impl.visualizations.DigitalLibraryVisualizationDataServiceImpl;
import rs.teslaris.reporting.service.interfaces.leaderboards.DocumentLeaderboardService;

@SpringBootTest
public class DigitalLibraryVisualizationDataServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private DocumentLeaderboardService documentLeaderboardService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private DigitalLibraryVisualizationDataServiceImpl service;


    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnThesisCountsForOrganisationUnit() throws IOException {
        // Given
        var organisationUnitId = 123;
        var startYear = 2020;
        var endYear = 2022;
        var allowedThesisTypes = List.of(ThesisType.MASTER, ThesisType.PHD);

        var mockMinAgg = mock(MinAggregate.class);
        when(mockMinAgg.value()).thenReturn(2020.0);

        var mockMaxAgg = mock(MaxAggregate.class);
        when(mockMaxAgg.value()).thenReturn(2022.0);

        Map<String, Aggregate> mockAggregations = new HashMap<>();
        var earliestYearAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        var latestYearAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(earliestYearAgg.min().value()).thenReturn(2020.0);
        when(latestYearAgg.max().value()).thenReturn(2022.0);
        mockAggregations.put("earliestYear", earliestYearAgg);
        mockAggregations.put("latestYear", latestYearAgg);

        var mockRangeResponse = mock(SearchResponse.class);
        when(mockRangeResponse.aggregations()).thenReturn(mockAggregations);

        var mockBucket = mock(StringTermsBucket.class);
        when(mockBucket.key()).thenReturn(FieldValue.of("MASTER"));
        when(mockBucket.docCount()).thenReturn(4L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.sterms().buckets().array()).thenReturn(List.of(mockBucket));

        mockAggregations.put("by_publication_type", mockAgg);

        var mockYearlyResponse = mock(SearchResponse.class);
        when(mockYearlyResponse.aggregations()).thenReturn(mockAggregations);

        when(organisationUnitService.findOne(anyInt())).thenReturn(new OrganisationUnit() {{
            setIsClientInstitutionDl(true);
        }});

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenReturn(mockRangeResponse);

        // When
        var result = service.getThesisCountsForOrganisationUnit(
            organisationUnitId, startYear, endYear, allowedThesisTypes);

        // Then
        assertEquals(3, result.size());
        assertEquals(2020, result.getFirst().year());
        assertEquals(1, result.getFirst().countsByCategory().size());
        assertEquals(4L, result.getFirst().countsByCategory().get("MASTER"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenNoThesesFoundForOrganisationUnit() throws IOException {
        // Given
        var organisationUnitId = 789;
        var startYear = 2020;
        var endYear = 2022;
        var allowedThesisTypes = List.of(ThesisType.MASTER);

        Map<String, Aggregate> mockAggregations = new HashMap<>();
        var earliestYearAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        var latestYearAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        var byTypeAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(earliestYearAgg.min().value()).thenReturn(Double.NaN);
        when(latestYearAgg.max().value()).thenReturn(Double.NaN);
        when(byTypeAgg.sterms().buckets().array()).thenReturn(List.of());
        mockAggregations.put("earliestYear", earliestYearAgg);
        mockAggregations.put("latestYear", latestYearAgg);
        mockAggregations.put("by_publication_type", byTypeAgg);

        var mockRangeResponse = mock(SearchResponse.class);
        when(mockRangeResponse.aggregations()).thenReturn(mockAggregations);

        when(organisationUnitService.findOne(anyInt())).thenReturn(new OrganisationUnit() {{
            setIsClientInstitutionDl(true);
        }});

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenReturn(mockRangeResponse);

        // When
        var result = service.getThesisCountsForOrganisationUnit(
            organisationUnitId, startYear, endYear, allowedThesisTypes);

        // Then
        result.forEach(yearlyCount -> assertTrue(yearlyCount.countsByCategory().isEmpty()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleLargeYearRangeEfficiently() throws IOException {
        // Given
        var organisationUnitId = 333;
        var startYear = 2010;
        var endYear = 2015;
        var allowedThesisTypes = List.of(ThesisType.MASTER);

        Map<String, Aggregate> mockAggregations = new HashMap<>();
        var masterBucket = mock(StringTermsBucket.class);
        when(masterBucket.key()).thenReturn(FieldValue.of("MASTER"));
        when(masterBucket.docCount()).thenReturn(8L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.sterms().buckets().array()).thenReturn(List.of(masterBucket));
        mockAggregations.put("by_publication_type", mockAgg);

        var mockYearlyResponse = mock(SearchResponse.class);
        when(mockYearlyResponse.aggregations()).thenReturn(mockAggregations);

        when(organisationUnitService.findOne(anyInt())).thenReturn(new OrganisationUnit() {{
            setIsClientInstitutionDl(true);
        }});

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenReturn(mockYearlyResponse) // 2010
            .thenReturn(mockYearlyResponse) // 2011
            .thenReturn(mockYearlyResponse) // 2012
            .thenReturn(mockYearlyResponse) // 2013
            .thenReturn(mockYearlyResponse) // 2014
            .thenReturn(mockYearlyResponse); // 2015

        // When
        var result = service.getThesisCountsForOrganisationUnit(
            organisationUnitId, startYear, endYear, allowedThesisTypes);

        // Then
        assertEquals(6, result.size());

        for (int i = 0; i < 6; i++) {
            assertEquals(2010 + i, result.get(i).year());
            assertEquals(8L, result.get(i).countsByCategory().get("MASTER"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnCountsForSingleYearWithMultipleThesisTypes() throws IOException {
        // Given
        var organisationUnitId = 111;
        var startYear = 2023;
        var endYear = 2023;
        var allowedThesisTypes =
            List.of(ThesisType.MASTER, ThesisType.PHD, ThesisType.UNDERGRADUATE_THESIS);

        Map<String, Aggregate> mockAggregations = new HashMap<>();

        var masterBucket = mock(StringTermsBucket.class);
        when(masterBucket.key()).thenReturn(FieldValue.of("MASTER"));
        when(masterBucket.docCount()).thenReturn(5L);

        var phdBucket = mock(StringTermsBucket.class);
        when(phdBucket.key()).thenReturn(FieldValue.of("PHD"));
        when(phdBucket.docCount()).thenReturn(3L);

        var undergraduateBucket = mock(StringTermsBucket.class);
        when(undergraduateBucket.key()).thenReturn(FieldValue.of("UNDERGRADUATE_THESIS"));
        when(undergraduateBucket.docCount()).thenReturn(1L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.sterms().buckets().array()).thenReturn(
            List.of(masterBucket, phdBucket, undergraduateBucket));
        mockAggregations.put("by_publication_type", mockAgg);

        var mockYearlyResponse = mock(SearchResponse.class);
        when(mockYearlyResponse.aggregations()).thenReturn(mockAggregations);

        when(organisationUnitService.findOne(anyInt())).thenReturn(new OrganisationUnit() {{
            setIsClientInstitutionDl(true);
        }});

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenReturn(mockYearlyResponse);

        // When
        var result = service.getThesisCountsForOrganisationUnit(
            organisationUnitId, startYear, endYear, allowedThesisTypes);

        // Then
        assertEquals(1, result.size());
        var yearlyCounts = result.getFirst();
        assertEquals(2023, yearlyCounts.year());

        var counts = yearlyCounts.countsByCategory();
        assertEquals(3, counts.size());
        assertEquals(5L, counts.get("MASTER"));
        assertEquals(3L, counts.get("PHD"));
        assertEquals(1L, counts.get("UNDERGRADUATE_THESIS"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnMonthlyStatisticsCountsForGivenPeriod() throws IOException {
        // Given
        var organisationUnitId = 123;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 3, 31);
        var statisticsType = StatisticsType.VIEW;
        var allowedThesisTypes = List.of(ThesisType.MASTER, ThesisType.PHD);

        var eligibleDocumentIds = List.of(1, 2, 3);
        when(documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
            allowedThesisTypes, 2023, 2023))
            .thenReturn(eligibleDocumentIds);

        Map<String, Aggregate> mockAggregations = new HashMap<>();

        var janBucket = mock(DateHistogramBucket.class);
        when(janBucket.keyAsString()).thenReturn("2023-01");
        when(janBucket.docCount()).thenReturn(150L);

        var febBucket = mock(DateHistogramBucket.class);
        when(febBucket.keyAsString()).thenReturn("2023-02");
        when(febBucket.docCount()).thenReturn(200L);

        var marBucket = mock(DateHistogramBucket.class);
        when(marBucket.keyAsString()).thenReturn("2023-03");
        when(marBucket.docCount()).thenReturn(180L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.dateHistogram().buckets().array()).thenReturn(
            List.of(janBucket, febBucket, marBucket));
        mockAggregations.put("per_month", mockAgg);

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(mockAggregations);

        when(organisationUnitService.findOne(anyInt())).thenReturn(new OrganisationUnit() {{
            setIsClientInstitutionDl(true);
        }});

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenReturn(mockResponse);

        // When
        var result =
            service.getMonthlyStatisticsCounts(organisationUnitId, from, to, statisticsType,
                allowedThesisTypes);

        // Then
        assertEquals(3, result.size());
        assertEquals(150L, result.get(YearMonth.of(2023, 1)));
        assertEquals(200L, result.get(YearMonth.of(2023, 2)));
        assertEquals(180L, result.get(YearMonth.of(2023, 3)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenNoEligibleDocuments() throws IOException {
        // Given
        var organisationUnitId = 456;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 3, 31);
        var statisticsType = StatisticsType.DOWNLOAD;
        var allowedThesisTypes = List.of(ThesisType.MASTER);

        when(documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
            allowedThesisTypes, 2023, 2023))
            .thenReturn(Collections.emptyList());

        // When
        var result =
            service.getMonthlyStatisticsCounts(organisationUnitId, from, to, statisticsType,
                allowedThesisTypes);

        // Then
        assertTrue(result.isEmpty());
        verify(elasticsearchClient, never()).search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleEmptyBucketsAndSkipNullKeys() throws IOException {
        // Given
        var organisationUnitId = 789;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 2, 28);
        var statisticsType = StatisticsType.VIEW;
        var allowedThesisTypes = List.of(ThesisType.PHD);

        var eligibleDocumentIds = List.of(4, 5);
        when(documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
            allowedThesisTypes, 2023, 2023))
            .thenReturn(eligibleDocumentIds);

        Map<String, Aggregate> mockAggregations = new HashMap<>();

        var janBucket = mock(DateHistogramBucket.class);
        when(janBucket.keyAsString()).thenReturn("2023-01");
        when(janBucket.docCount()).thenReturn(100L);

        var nullKeyBucket = mock(DateHistogramBucket.class);
        when(nullKeyBucket.keyAsString()).thenReturn(null);
        when(nullKeyBucket.docCount()).thenReturn(50L);

        var febBucket = mock(DateHistogramBucket.class);
        when(febBucket.keyAsString()).thenReturn("2023-02");
        when(febBucket.docCount()).thenReturn(75L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.dateHistogram().buckets().array()).thenReturn(
            List.of(janBucket, nullKeyBucket, febBucket));
        mockAggregations.put("per_month", mockAgg);

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(mockAggregations);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenReturn(mockResponse);

        when(organisationUnitService.findOne(anyInt())).thenReturn(new OrganisationUnit() {{
            setIsClientInstitutionDl(true);
        }});

        // When
        var result =
            service.getMonthlyStatisticsCounts(organisationUnitId, from, to, statisticsType,
                allowedThesisTypes);

        // Then
        assertEquals(2, result.size());
        assertEquals(100L, result.get(YearMonth.of(2023, 1)));
        assertEquals(75L, result.get(YearMonth.of(2023, 2)));
        assertFalse(result.containsValue(50L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapOnIOException() throws IOException {
        // Given
        var organisationUnitId = 999;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 1, 31);
        var statisticsType = StatisticsType.DOWNLOAD;
        var allowedThesisTypes = List.of(ThesisType.MASTER);

        var eligibleDocumentIds = List.of(6, 7);
        when(documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
            allowedThesisTypes, 2023, 2023))
            .thenReturn(eligibleDocumentIds);

        when(organisationUnitService.findOne(anyInt())).thenReturn(new OrganisationUnit() {{
            setIsClientInstitutionDl(true);
        }});

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenThrow(new IOException("Elasticsearch connection failed"));

        // When
        var result =
            service.getMonthlyStatisticsCounts(organisationUnitId, from, to, statisticsType,
                allowedThesisTypes);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnByCountryStatisticsForDigitalLibrary() throws IOException {
        // Given
        var organisationUnitId = 123;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 12, 31);
        var statisticsType = StatisticsType.VIEW;
        var allowedThesisTypes = List.of(ThesisType.MASTER, ThesisType.PHD);

        var eligibleDocumentIds = List.of(1, 2, 3, 4);
        when(documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
            allowedThesisTypes, 2023, 2023))
            .thenReturn(eligibleDocumentIds);

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setIsClientInstitutionDl(true);
            }});

        var serbiaNameBucket = mock(StringTermsBucket.class);
        when(serbiaNameBucket.key()).thenReturn(FieldValue.of("Serbia"));

        var usaNameBucket = mock(StringTermsBucket.class);
        when(usaNameBucket.key()).thenReturn(FieldValue.of("United States"));

        var germanyNameBucket = mock(StringTermsBucket.class);
        when(germanyNameBucket.key()).thenReturn(FieldValue.of("Germany"));

        var serbiaNameAggregate = mockAggregateWithBuckets(List.of(serbiaNameBucket));
        var usaNameAggregate = mockAggregateWithBuckets(List.of(usaNameBucket));
        var germanyNameAggregate = mockAggregateWithBuckets(List.of(germanyNameBucket));

        var serbiaBucket = mock(StringTermsBucket.class);
        when(serbiaBucket.key()).thenReturn(FieldValue.of("RS"));
        when(serbiaBucket.docCount()).thenReturn(150L);
        when(serbiaBucket.aggregations()).thenReturn(Map.of("country_name", serbiaNameAggregate));

        var usaBucket = mock(StringTermsBucket.class);
        when(usaBucket.key()).thenReturn(FieldValue.of("US"));
        when(usaBucket.docCount()).thenReturn(75L);
        when(usaBucket.aggregations()).thenReturn(Map.of("country_name", usaNameAggregate));

        var germanyBucket = mock(StringTermsBucket.class);
        when(germanyBucket.key()).thenReturn(FieldValue.of("DE"));
        when(germanyBucket.docCount()).thenReturn(50L);
        when(germanyBucket.aggregations()).thenReturn(Map.of("country_name", germanyNameAggregate));

        var countryAggregate = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(countryAggregate.sterms().buckets().array()).thenReturn(
            List.of(serbiaBucket, usaBucket, germanyBucket));

        Map<String, Aggregate> mockAggregations = Map.of("by_country", countryAggregate);

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(mockAggregations);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenReturn(mockResponse);

        // When
        var result = service.getByCountryStatisticsForDigitalLibrary(
            organisationUnitId, from, to, statisticsType, allowedThesisTypes);

        // Then
        assertEquals(3, result.size());
    }

    private Aggregate mockAggregateWithBuckets(List<StringTermsBucket> buckets) {
        var aggregate = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(aggregate.sterms().buckets().array()).thenReturn(buckets);
        return aggregate;
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenNoEligibleDocuments() throws IOException {
        // Given
        var organisationUnitId = 456;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 12, 31);
        var statisticsType = StatisticsType.DOWNLOAD;
        var allowedThesisTypes = List.of(ThesisType.MASTER);

        when(documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
            allowedThesisTypes, 2023, 2023))
            .thenReturn(Collections.emptyList());

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setIsClientInstitutionDl(true);
            }});

        // When
        var result = service.getByCountryStatisticsForDigitalLibrary(
            organisationUnitId, from, to, statisticsType, allowedThesisTypes);

        // Then
        assertTrue(result.isEmpty());
        verify(elasticsearchClient, never()).search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenInstitutionNotDigitalLibrary() throws IOException {
        // Given
        var organisationUnitId = 789;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 12, 31);
        var statisticsType = StatisticsType.VIEW;
        var allowedThesisTypes = List.of(ThesisType.PHD);

        var eligibleDocumentIds = List.of(5, 6, 7);
        when(documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
            allowedThesisTypes, 2023, 2023))
            .thenReturn(eligibleDocumentIds);

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setIsClientInstitutionDl(false);
            }});

        // When
        var result = service.getByCountryStatisticsForDigitalLibrary(
            organisationUnitId, from, to, statisticsType, allowedThesisTypes);

        // Then
        assertTrue(result.isEmpty());
        verify(elasticsearchClient, never()).search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleEmptyCountryBuckets() throws IOException {
        // Given
        var organisationUnitId = 111;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 12, 31);
        var statisticsType = StatisticsType.DOWNLOAD;
        var allowedThesisTypes = List.of(ThesisType.MASTER, ThesisType.PHD);

        var eligibleDocumentIds = List.of(8, 9);
        when(documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
            allowedThesisTypes, 2023, 2023))
            .thenReturn(eligibleDocumentIds);

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setIsClientInstitutionDl(true);
            }});

        var countryAggregate = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(countryAggregate.sterms().buckets().array()).thenReturn(Collections.emptyList());

        Map<String, Aggregate> mockAggregations = Map.of("by_country", countryAggregate);

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(mockAggregations);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenReturn(mockResponse);

        // When
        var result = service.getByCountryStatisticsForDigitalLibrary(
            organisationUnitId, from, to, statisticsType, allowedThesisTypes);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldUseCountryCodeAsFallbackWhenCountryNameMissing() throws IOException {
        // Given
        var organisationUnitId = 222;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 12, 31);
        var statisticsType = StatisticsType.VIEW;
        var allowedThesisTypes = List.of(ThesisType.MASTER);

        var eligibleDocumentIds = List.of(10);
        when(documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
            allowedThesisTypes, 2023, 2023))
            .thenReturn(eligibleDocumentIds);

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setIsClientInstitutionDl(true);
            }});

        var franceBucket = mock(StringTermsBucket.class);
        when(franceBucket.key()).thenReturn(FieldValue.of("FR"));
        when(franceBucket.docCount()).thenReturn(25L);

        var aggregate = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(aggregate.sterms().buckets().array()).thenReturn(Collections.emptyList());
        when(franceBucket.aggregations()).thenReturn(Map.of("country_name",
            aggregate));

        var countryAggregate = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(countryAggregate.sterms().buckets().array()).thenReturn(List.of(franceBucket));

        Map<String, Aggregate> mockAggregations = Map.of("by_country", countryAggregate);

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(mockAggregations);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenReturn(mockResponse);

        // When
        var result = service.getByCountryStatisticsForDigitalLibrary(
            organisationUnitId, from, to, statisticsType, allowedThesisTypes);

        // Then
        assertEquals(1, result.size());
        var franceStats = result.getFirst();
        assertEquals("FR", franceStats.countryCode());
        assertEquals("FR", franceStats.countryName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListOnIOException() throws IOException {
        // Given
        var organisationUnitId = 333;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 12, 31);
        var statisticsType = StatisticsType.DOWNLOAD;
        var allowedThesisTypes = List.of(ThesisType.PHD);

        var eligibleDocumentIds = List.of(11, 12);
        when(documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
            allowedThesisTypes, 2023, 2023))
            .thenReturn(eligibleDocumentIds);

        when(organisationUnitService.findOne(organisationUnitId)).thenReturn(
            new OrganisationUnit() {{
                setIsClientInstitutionDl(true);
            }});

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(), eq(Void.class)))
            .thenThrow(new IOException("Elasticsearch connection failed"));

        // When
        var result = service.getByCountryStatisticsForDigitalLibrary(
            organisationUnitId, from, to, statisticsType, allowedThesisTypes);

        // Then
        assertTrue(result.isEmpty());
    }
}
