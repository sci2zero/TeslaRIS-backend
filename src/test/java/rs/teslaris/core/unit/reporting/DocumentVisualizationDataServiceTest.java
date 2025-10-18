package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.reporting.service.impl.visualizations.DocumentVisualizationDataServiceImpl;

@SpringBootTest
class DocumentVisualizationDataServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private SearchService<DocumentPublicationIndex> searchService;

    @InjectMocks
    private DocumentVisualizationDataServiceImpl service;


    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnCountryStatistics() throws IOException {
        // Given
        var documentId = 123;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 12, 31);
        var statisticsType = StatisticsType.VIEW;

        when(documentPublicationService.findOne(documentId)).thenReturn(
            new ProceedingsPublication() {{
                setMergedIds(new HashSet<>(List.of(456, 789)));
            }}
        );

        // Mock sub-aggregation for country_name
        var mockSubBucket = mock(StringTermsBucket.class);
        when(mockSubBucket.key()).thenReturn(FieldValue.of("Serbia"));

        var mockCountryNameAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockCountryNameAgg.sterms().buckets().array()).thenReturn(List.of(mockSubBucket));

        // Mock main country bucket
        var mockCountryBucket = mock(StringTermsBucket.class, RETURNS_DEEP_STUBS);
        when(mockCountryBucket.key().stringValue()).thenReturn("RS");
        when(mockCountryBucket.docCount()).thenReturn(42L);
        when(mockCountryBucket.aggregations()).thenReturn(
            Map.of("country_name", mockCountryNameAgg));

        var mockByCountryAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockByCountryAgg.sterms().buckets().array()).thenReturn(List.of(mockCountryBucket));

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(Map.of("by_country", mockByCountryAgg));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenReturn(
            mockResponse
        );

        // When
        var result =
            service.getByCountryStatisticsForDocument(documentId, from, to, statisticsType);

        // Then
        assertEquals(1, result.size());
        var stats = result.getFirst();
        assertEquals("RS", stats.countryCode());
        assertEquals("Serbia", stats.countryName());
        assertEquals(42L, stats.value());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenIOException() throws IOException {
        // Given
        var documentId = 123;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 12, 31);
        var statisticsType = StatisticsType.VIEW;

        when(documentPublicationService.findOne(documentId)).thenReturn(
            new JournalPublication() {{
                setMergedIds(new HashSet<>(List.of()));
            }}
        );

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenThrow(
            new IOException()
        );

        // When
        var result =
            service.getByCountryStatisticsForDocument(documentId, from, to, statisticsType);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnMonthlyStatisticsCounts() throws IOException {
        // Given
        var documentId = 123;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 3, 31);
        var statisticsType = StatisticsType.VIEW;

        when(documentPublicationService.findOne(documentId)).thenReturn(
            new Software() {{
                setMergedIds(new HashSet<>(List.of(456, 789)));
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

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenReturn(mockResponse);

        // When
        var result = service.getMonthlyStatisticsCounts(documentId, from, to, statisticsType);

        // Then
        assertEquals(3, result.size());
        assertEquals(15L, result.get(YearMonth.of(2023, 1)));
        assertEquals(20L, result.get(YearMonth.of(2023, 2)));
        assertEquals(25L, result.get(YearMonth.of(2023, 3)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenIOExceptionInMonthlyStatistics() throws IOException {
        // Given
        var documentId = 123;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 3, 31);
        var statisticsType = StatisticsType.VIEW;

        when(documentPublicationService.findOne(documentId)).thenReturn(
            new Dataset() {{
                setMergedIds(new HashSet<>(List.of()));
            }}
        );

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenThrow(new IOException());

        // When
        var result = service.getMonthlyStatisticsCounts(documentId, from, to, statisticsType);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnYearlyStatisticsCounts() throws IOException {
        // Given
        var documentId = 123;
        var startYear = 2022;
        var endYear = 2023;
        var statisticsType = StatisticsType.VIEW;

        when(documentPublicationService.findOne(documentId)).thenReturn(
            new Patent() {{
                setMergedIds(new HashSet<>(List.of(456, 789)));
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

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenReturn(mockResponse);

        // When
        var result =
            service.getYearlyStatisticsCounts(documentId, startYear, endYear, statisticsType);

        // Then
        assertEquals(2, result.size());
        assertEquals(30L, result.get(Year.of(2022)));
        assertEquals(45L, result.get(Year.of(2023)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyMapWhenIOExceptionInYearlyStatistics() throws IOException {
        // Given
        var documentId = 123;
        var startYear = 2022;
        var endYear = 2023;
        var statisticsType = StatisticsType.VIEW;

        when(documentPublicationService.findOne(documentId)).thenReturn(
            new MonographPublication() {{
                setMergedIds(new HashSet<>(List.of()));
            }}
        );

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenThrow(new IOException());

        // When
        var result =
            service.getYearlyStatisticsCounts(documentId, startYear, endYear, statisticsType);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    public void shouldRunQueryForPersonId() {
        // Given
        var type = DocumentPublicationType.DATASET;
        var yearFrom = 2020;
        var yearTo = 2023;
        var personId = 10;
        Integer institutionId = null;
        var pageable = PageRequest.of(0, 10);
        var expectedPage =
            new PageImpl<>(List.of(new DocumentPublicationIndex()));

        when(searchService.runQuery(any(Query.class), eq(pageable),
            eq(DocumentPublicationIndex.class), eq("document_publication")))
            .thenReturn(expectedPage);

        // When
        var result = service.findPublicationsForTypeAndPeriod(
            type, yearFrom, yearTo, personId, institutionId, pageable);

        // Then
        verify(searchService, times(1))
            .runQuery(any(Query.class), eq(pageable),
                eq(DocumentPublicationIndex.class), eq("document_publication"));
        assertEquals(expectedPage, result);
    }

    @Test
    public void shouldRunQueryForInstitutionId() {
        // Given
        var type = DocumentPublicationType.MONOGRAPH;
        var yearFrom = 2015;
        var yearTo = 2020;
        Integer personId = null;
        var institutionId = 5;
        var pageable = PageRequest.of(0, 5);
        var expectedPage = new PageImpl<>(List.of(new DocumentPublicationIndex()));

        when(searchService.runQuery(any(Query.class), eq(pageable),
            eq(DocumentPublicationIndex.class), eq("document_publication")))
            .thenReturn(expectedPage);

        // When
        var result = service.findPublicationsForTypeAndPeriod(
            type, yearFrom, yearTo, personId, institutionId, pageable);

        // Then
        verify(searchService, times(1))
            .runQuery(any(Query.class), eq(pageable),
                eq(DocumentPublicationIndex.class), eq("document_publication"));
        assertEquals(expectedPage, result);
    }

    @Test
    public void shouldThrowExceptionWhenPersonAndInstitutionIdsAreNull() {
        // Given
        var type = DocumentPublicationType.SOFTWARE;
        var yearFrom = 2019;
        var yearTo = 2022;
        Integer personId = null;
        Integer institutionId = null;
        var pageable = PageRequest.of(0, 10);

        // When / Then
        assertThrows(IllegalArgumentException.class, () ->
            service.findPublicationsForTypeAndPeriod(
                type, yearFrom, yearTo, personId, institutionId, pageable)
        );

        verifyNoInteractions(searchService);
    }
}
