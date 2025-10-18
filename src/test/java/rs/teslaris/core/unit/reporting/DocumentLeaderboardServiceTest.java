package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.impl.leaderboards.DocumentLeaderboardServiceImpl;
import rs.teslaris.reporting.utility.QueryUtil;

@SpringBootTest
public class DocumentLeaderboardServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @InjectMocks
    private DocumentLeaderboardServiceImpl documentLeaderboardService;


    private static Stream<Arguments> provideAggregationEdgeCases() {
        return Stream.of(
            Arguments.of(Double.NaN, 0L, true),          // NaN should become 0
            Arguments.of(0.0, 0L, false),                // Zero value
            Arguments.of(123.45, 123L, true),            // Double should be cast to long
            Arguments.of(999999.99, 999999L, false)      // Large value
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnPublicationsWithMostCitations() throws IOException {
        // Given
        var institutionId = 1;
        var fromYear = 2020;
        var toYear = 2024;

        try (var queryUtilMock = mockStatic(QueryUtil.class)) {
            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(fromYear, toYear));
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("org_field1", "org_field2"));
            queryUtilMock.when(() -> QueryUtil.organisationUnitMatchQuery(any(), any()))
                .thenReturn(mock(Query.class));

            when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
                .thenReturn(List.of(1, 2, 3));

            var publication1 = new DocumentPublicationIndex();
            publication1.setDatabaseId(101);
            publication1.setApa("Paper 1");
            var hit1 = mock(Hit.class);
            when(hit1.source()).thenReturn(publication1);

            var publication2 = new DocumentPublicationIndex();
            publication2.setDatabaseId(102);
            publication2.setApa("Paper 2");
            var hit2 = mock(Hit.class);
            when(hit2.source()).thenReturn(publication2);

            var mockHitsMetadata = mock(HitsMetadata.class);
            when(mockHitsMetadata.hits()).thenReturn(List.of(hit1, hit2));

            var mockBucket1 = mock(LongTermsBucket.class, RETURNS_DEEP_STUBS);
            when(mockBucket1.key()).thenReturn(101L);
            when(mockBucket1.aggregations().get("by_citation_count").sum().value()).thenReturn(
                150.0);

            var mockBucket2 = mock(LongTermsBucket.class, RETURNS_DEEP_STUBS);
            when(mockBucket2.key()).thenReturn(102L);
            when(mockBucket2.aggregations().get("by_citation_count").sum().value()).thenReturn(
                200.0);

            var mockLongTerms = mock(LongTermsAggregate.class, RETURNS_DEEP_STUBS);
            when(mockLongTerms.buckets().array()).thenReturn(List.of(mockBucket1, mockBucket2));

            var mockAggregate = mock(Aggregate.class, RETURNS_DEEP_STUBS);
            when(mockAggregate.lterms()).thenReturn(mockLongTerms);

            var mockAggregations = new HashMap<String, Aggregate>();
            mockAggregations.put("top_documents", mockAggregate);

            var mockResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
            when(mockResponse.hits()).thenReturn(mockHitsMetadata);
            when(mockResponse.aggregations()).thenReturn(mockAggregations);

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(DocumentPublicationIndex.class))
            ).thenReturn(mockResponse);

            // When
            var result =
                documentLeaderboardService.getPublicationsWithMostCitations(institutionId, fromYear,
                    toYear);

            // Then
            assertEquals(2, result.size());
            assertEquals(200L, result.get(0).b);
            assertEquals(150L, result.get(1).b);
            assertEquals("Paper 2", result.get(0).a.getApa());
            assertEquals("Paper 1", result.get(1).a.getApa());

            assertTrue(result.get(0).b >= result.get(1).b);
        }
    }

    @Test
    void shouldReturnEmptyListWhenYearRangeInvalid() {
        // Given
        var institutionId = 1;
        Integer fromYear = null;
        Integer toYear = null;

        try (var queryUtilMock = mockStatic(QueryUtil.class)) {
            queryUtilMock.when(() -> QueryUtil.constructYearRange(fromYear, toYear))
                .thenReturn(new Pair<>(null, null));

            // When
            var result =
                documentLeaderboardService.getPublicationsWithMostCitations(institutionId, fromYear,
                    toYear);

            // Then
            assertTrue(result.isEmpty());
            verifyNoInteractions(elasticsearchClient, organisationUnitService);
        }
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

    @ParameterizedTest
    @EnumSource(StatisticsType.class)
    @SuppressWarnings("unchecked")
    void shouldReturnTopPublicationsForAllStatisticsTypes(StatisticsType statisticsType)
        throws IOException {
        // Given
        var institutionId = 123;
        var fromDate = LocalDate.of(2023, 1, 1);
        var toDate = LocalDate.of(2023, 12, 31);

        var mockDocument1 = new DocumentPublicationIndex();
        mockDocument1.setDatabaseId(1);
        var mockDocument2 = new DocumentPublicationIndex();
        mockDocument2.setDatabaseId(2);

        var mockDocumentHit1 = mock(Hit.class);
        when(mockDocumentHit1.source()).thenReturn(mockDocument1);
        var mockDocumentHit2 = mock(Hit.class);
        when(mockDocumentHit2.source()).thenReturn(mockDocument2);

        var mockDocumentResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockDocumentResponse.hits().hits()).thenReturn(
            Arrays.asList(mockDocumentHit1, mockDocumentHit2));

        var mockBucket1 = mock(LongTermsBucket.class, RETURNS_DEEP_STUBS);
        when(mockBucket1.key()).thenReturn(1L);
        when(mockBucket1.aggregations().get("stat_count").valueCount().value()).thenReturn(150.0);

        var mockBucket2 = mock(LongTermsBucket.class, RETURNS_DEEP_STUBS);
        when(mockBucket2.key()).thenReturn(2L);
        when(mockBucket2.aggregations().get("stat_count").valueCount().value()).thenReturn(100.0);

        var buckets = Arrays.asList(mockBucket1, mockBucket2);
        var mockLongTerms = mock(LongTermsAggregate.class, RETURNS_DEEP_STUBS);
        when(mockLongTerms.buckets().array()).thenReturn(buckets);

        var mockAggregate = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate.lterms()).thenReturn(mockLongTerms);

        var mockAggregations = new HashMap<String, Aggregate>();
        mockAggregations.put("by_document", mockAggregate);

        var mockStatsResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockStatsResponse.aggregations()).thenReturn(mockAggregations);

        var mockDetailedDocument1 = new DocumentPublicationIndex();
        mockDetailedDocument1.setDatabaseId(1);

        var mockDetailedDocument2 = new DocumentPublicationIndex();
        mockDetailedDocument2.setDatabaseId(2);

        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(1))
            .thenReturn(Optional.of(mockDetailedDocument1));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(2))
            .thenReturn(Optional.of(mockDetailedDocument2));

        try (var queryUtilMock = mockStatic(QueryUtil.class)) {
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("org_field1", "org_field2"));
            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(institutionId))
                .thenReturn(List.of(123, 456));
            queryUtilMock.when(() -> QueryUtil.organisationUnitMatchQuery(any(), any()))
                .thenReturn(mock(Query.class));

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(DocumentPublicationIndex.class)
            )).thenReturn(mockDocumentResponse);

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class)
            )).thenReturn(mockStatsResponse);

            // When
            var result =
                documentLeaderboardService.getTopPublicationsByStatisticCount(institutionId,
                    statisticsType, fromDate, toDate, false, Collections.emptyList());

            // Then
            assertEquals(2, result.size());
            assertEquals(150L, result.get(0).b);
            assertEquals(100L, result.get(1).b);
        }
    }

    @ParameterizedTest
    @MethodSource("provideAggregationEdgeCases")
    @SuppressWarnings("unchecked")
    void shouldHandleAggregationEdgeCases(Double aggregationValue, long expectedCount,
                                          boolean onlyTheses)
        throws IOException {
        // Given
        var institutionId = 123;
        var statisticsType = StatisticsType.VIEW;
        var fromDate = LocalDate.of(2023, 1, 1);
        var toDate = LocalDate.of(2023, 12, 31);

        when(organisationUnitService.findOne(any())).thenReturn(new OrganisationUnit() {{
            setIsClientInstitutionDl(true);
        }});

        var mockDocument = new DocumentPublicationIndex();
        mockDocument.setDatabaseId(1);
        var mockDocumentHit = mock(Hit.class);
        when(mockDocumentHit.source()).thenReturn(mockDocument);

        var mockDocumentResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockDocumentResponse.hits().hits()).thenReturn(List.of(mockDocumentHit));

        var mockBucket = mock(LongTermsBucket.class, RETURNS_DEEP_STUBS);
        when(mockBucket.key()).thenReturn(1L);
        when(mockBucket.aggregations().get("stat_count").valueCount().value()).thenReturn(
            aggregationValue);

        var mockLongTerms = mock(LongTermsAggregate.class, RETURNS_DEEP_STUBS);
        when(mockLongTerms.buckets().array()).thenReturn(List.of(mockBucket));

        var mockAggregate = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAggregate.lterms()).thenReturn(mockLongTerms);

        var mockAggregations = new HashMap<String, Aggregate>();
        mockAggregations.put("by_document", mockAggregate);

        var mockStatsResponse = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(mockStatsResponse.aggregations()).thenReturn(mockAggregations);

        var mockDetailedDocument = new DocumentPublicationIndex();
        mockDetailedDocument.setDatabaseId(1);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(1))
            .thenReturn(Optional.of(mockDetailedDocument));

        try (var queryUtilMock = mockStatic(QueryUtil.class)) {
            queryUtilMock.when(() -> QueryUtil.getOrganisationUnitOutputSearchFields(institutionId))
                .thenReturn(List.of("org_field1"));
            queryUtilMock.when(() -> QueryUtil.getAllMergedOrganisationUnitIds(institutionId))
                .thenReturn(List.of(123));
            queryUtilMock.when(() -> QueryUtil.organisationUnitMatchQuery(any(), any()))
                .thenReturn(mock(Query.class));

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(DocumentPublicationIndex.class)
            )).thenReturn(mockDocumentResponse);

            when(elasticsearchClient.search(
                (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
                eq(Void.class)
            )).thenReturn(mockStatsResponse);

            // When
            var result =
                documentLeaderboardService.getTopPublicationsByStatisticCount(institutionId,
                    statisticsType, fromDate, toDate, onlyTheses, List.of(ThesisType.PHD));

            // Then
            assertEquals(1, result.size());
            assertEquals(expectedCount, result.getFirst().b);
        }
    }
}
