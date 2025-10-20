package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.reporting.service.impl.leaderboards.GlobalLeaderboardServiceImpl;

@SpringBootTest
class GlobalLeaderboardServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private OrganisationUnitIndexRepository organisationUnitIndexRepository;

    @InjectMocks
    private GlobalLeaderboardServiceImpl service;


    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnPersonsWithMostCitations() throws IOException {
        // Given
        var eligibleOU = new OrganisationUnitIndex();
        eligibleOU.setDatabaseId(1);
        var ouHit = mock(Hit.class);
        when(ouHit.source()).thenReturn(eligibleOU);

        var ouHitsMetadata = mock(HitsMetadata.class);
        when(ouHitsMetadata.hits()).thenReturn(List.of(ouHit));

        var ouSearchResponse = mock(SearchResponse.class);
        when(ouSearchResponse.hits()).thenReturn(ouHitsMetadata);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(OrganisationUnitIndex.class)))
            .thenReturn(ouSearchResponse);

        var bucket1 = mock(LongTermsBucket.class);
        when(bucket1.key()).thenReturn(1L);

        var sumAgg1 = mock(SumAggregate.class);
        when(sumAgg1.value()).thenReturn(120.0);
        var citationsAgg1 = mock(Aggregate.class);
        when(citationsAgg1.sum()).thenReturn(sumAgg1);

        var hit1 = mock(Hit.class);
        var jsonData1 = mock(JsonData.class);
        when(jsonData1.to(Map.class)).thenReturn(Map.of(
            "databaseId", 1,
            "name", "Alice"
        ));
        when(hit1.source()).thenReturn(jsonData1);

        var hits1 = mock(HitsMetadata.class);
        when(hits1.hits()).thenReturn(List.of(hit1));

        var topHitsAgg1 = mock(TopHitsAggregate.class);
        when(topHitsAgg1.hits()).thenReturn(hits1);

        var topHitsAggregate1 = mock(Aggregate.class);
        when(topHitsAggregate1.topHits()).thenReturn(topHitsAgg1);

        when(bucket1.aggregations()).thenReturn(Map.of(
            "total_citations", citationsAgg1,
            "top_hits", topHitsAggregate1
        ));

        var bucket2 = mock(LongTermsBucket.class);
        when(bucket2.key()).thenReturn(2L);

        var sumAgg2 = mock(SumAggregate.class);
        when(sumAgg2.value()).thenReturn(80.0);
        var citationsAgg2 = mock(Aggregate.class);
        when(citationsAgg2.sum()).thenReturn(sumAgg2);

        var hit2 = mock(Hit.class);
        var jsonData2 = mock(JsonData.class);
        when(jsonData2.to(Map.class)).thenReturn(Map.of(
            "databaseId", 2,
            "name", "Bob"
        ));
        when(hit2.source()).thenReturn(jsonData2);

        var hits2 = mock(HitsMetadata.class);
        when(hits2.hits()).thenReturn(List.of(hit2));

        var topHitsAgg2 = mock(TopHitsAggregate.class);
        when(topHitsAgg2.hits()).thenReturn(hits2);

        var topHitsAggregate2 = mock(Aggregate.class);
        when(topHitsAggregate2.topHits()).thenReturn(topHitsAgg2);

        when(bucket2.aggregations()).thenReturn(Map.of(
            "total_citations", citationsAgg2,
            "top_hits", topHitsAggregate2
        ));

        var termsAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(termsAgg.lterms().buckets().array()).thenReturn(List.of(bucket1, bucket2));

        var response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(response.aggregations()).thenReturn(Map.of("by_person", termsAgg));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(PersonIndex.class)))
            .thenReturn(response);

        // When
        var result = service.getPersonsWithMostCitations();

        // Then
        assertEquals(2, result.size());
        assertEquals(1, result.getFirst().a.getDatabaseId());
        assertEquals("Alice", result.getFirst().a.getName());
        assertEquals(120L, result.getFirst().b);
        assertEquals(2, result.get(1).a.getDatabaseId());
        assertEquals("Bob", result.get(1).a.getName());
        assertEquals(80L, result.get(1).b);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnInstitutionsWithMostCitations() throws IOException {
        // Given
        var eligibleOU = new OrganisationUnitIndex();
        eligibleOU.setDatabaseId(1);
        var ouHit = mock(Hit.class);
        when(ouHit.source()).thenReturn(eligibleOU);

        var ouHitsMetadata = mock(HitsMetadata.class);
        when(ouHitsMetadata.hits()).thenReturn(List.of(ouHit));

        var ouSearchResponse = mock(SearchResponse.class);
        when(ouSearchResponse.hits()).thenReturn(ouHitsMetadata);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(OrganisationUnitIndex.class)))
            .thenReturn(ouSearchResponse);

        var bucket1 = mock(LongTermsBucket.class);
        when(bucket1.key()).thenReturn(10L);

        var sumAgg1 = mock(SumAggregate.class);
        when(sumAgg1.value()).thenReturn(300.0);
        var agg1 = mock(Aggregate.class);
        when(agg1.sum()).thenReturn(sumAgg1);
        when(bucket1.aggregations()).thenReturn(Map.of("total_citations", agg1));

        var termsAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(termsAgg.lterms().buckets().array()).thenReturn(List.of(bucket1));

        var response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(response.aggregations()).thenReturn(Map.of("by_org_unit", termsAgg));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class)))
            .thenReturn(response);

        var ou = new OrganisationUnitIndex();
        ou.setDatabaseId(10);
        ou.setNameSr("Test Institution");
        when(organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(10))
            .thenReturn(Optional.of(ou));

        // When
        var result = service.getInstitutionsWithMostCitations();

        // Then
        assertEquals(1, result.size());
        assertEquals(ou, result.getFirst().a);
        assertEquals(300L, result.getFirst().b);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnDocumentsWithMostCitations() throws IOException {
        // Given
        var bucket1 = mock(LongTermsBucket.class);
        when(bucket1.key()).thenReturn(100L);

        var sumAgg1 = mock(SumAggregate.class);
        when(sumAgg1.value()).thenReturn(45.0);
        var citationsAgg1 = mock(Aggregate.class);
        when(citationsAgg1.sum()).thenReturn(sumAgg1);

        var hit1 = mock(Hit.class);
        var doc1 = new DocumentPublicationIndex();
        doc1.setDatabaseId(100);
        doc1.setTitleOther("Doc A");

        var jsonData1 = mock(JsonData.class);
        when(jsonData1.to(any(Class.class))).thenReturn(doc1);
        when(hit1.source()).thenReturn(jsonData1);

        var hits1 = mock(HitsMetadata.class);
        when(hits1.hits()).thenReturn(List.of(hit1));

        var topHitsAgg1 = mock(TopHitsAggregate.class);
        when(topHitsAgg1.hits()).thenReturn(hits1);

        var topHitsAggregate1 = mock(Aggregate.class);
        when(topHitsAggregate1.topHits()).thenReturn(topHitsAgg1);

        when(bucket1.aggregations()).thenReturn(Map.of(
            "total_citations", citationsAgg1,
            "top_hits", topHitsAggregate1
        ));

        var bucket2 = mock(LongTermsBucket.class);
        when(bucket2.key()).thenReturn(200L);

        var sumAgg2 = mock(SumAggregate.class);
        when(sumAgg2.value()).thenReturn(60.0);
        var citationsAgg2 = mock(Aggregate.class);
        when(citationsAgg2.sum()).thenReturn(sumAgg2);

        var hit2 = mock(Hit.class);
        var doc2 = new DocumentPublicationIndex();
        doc2.setDatabaseId(200);
        doc2.setTitleOther("Doc B");

        var jsonData2 = mock(JsonData.class);
        when(jsonData2.to(any(Class.class))).thenReturn(doc2);
        when(hit2.source()).thenReturn(jsonData2);

        var hits2 = mock(HitsMetadata.class);
        when(hits2.hits()).thenReturn(List.of(hit2));

        var topHitsAgg2 = mock(TopHitsAggregate.class);
        when(topHitsAgg2.hits()).thenReturn(hits2);

        var topHitsAggregate2 = mock(Aggregate.class);
        when(topHitsAggregate2.topHits()).thenReturn(topHitsAgg2);

        when(bucket2.aggregations()).thenReturn(Map.of(
            "total_citations", citationsAgg2,
            "top_hits", topHitsAggregate2
        ));

        var termsAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(termsAgg.lterms().buckets().array()).thenReturn(List.of(bucket1, bucket2));

        var response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        when(response.aggregations()).thenReturn(Map.of("by_publication", termsAgg));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(DocumentPublicationIndex.class))).thenReturn(response);

        // When
        var result = service.getDocumentsWithMostCitations();

        // Then
        assertEquals(2, result.size());
        assertEquals(60L, result.getFirst().b);
        assertEquals(45L, result.get(1).b);
    }
}
