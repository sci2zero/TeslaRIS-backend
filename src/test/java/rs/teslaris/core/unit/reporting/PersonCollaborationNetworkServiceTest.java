package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.reporting.service.impl.PersonCollaborationNetworkServiceImpl;

@SpringBootTest
class PersonCollaborationNetworkServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private PersonIndexRepository personIndexRepository;

    @InjectMocks
    private PersonCollaborationNetworkServiceImpl collaborationService;


    @Test
    void shouldReturnEmptyNetworkWhenDepthIsInvalid() {
        var result = collaborationService.findCollaborationNetwork(1, 0);
        assertNotNull(result);
        assertTrue(result.nodes().isEmpty());
        assertTrue(result.links().isEmpty());
    }

    @Test
    void shouldReturnEmptyNetworkWhenEsThrowsException() throws IOException {
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Void.class)))
            .thenThrow(new IOException("ES failure"));

        var result = collaborationService.findCollaborationNetwork(1, 1);

        assertNotNull(result);
        assertTrue(result.nodes().isEmpty());
        assertTrue(result.links().isEmpty());
    }

    @Test
    void shouldReturnNetworkWithSingleNodeWhenNoCollaborators() throws IOException {
        var emptyAgg = new LongTermsAggregate.Builder()
            .buckets(new Buckets.Builder<LongTermsBucket>().array(List.of()).build())
            .build();

        var response = mock(SearchResponse.class);
        when(response.aggregations()).thenReturn(
            Map.of("coauthors", Aggregate.of(a -> a.lterms(emptyAgg)))
        );
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Void.class)))
            .thenReturn(response);

        var person = new PersonIndex();
        person.setDatabaseId(1);
        person.setName("Dr. Solo");
        when(personIndexRepository.findByDatabaseIdIn(anyList(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(person)));

        var result = collaborationService.findCollaborationNetwork(1, 1);

        assertNotNull(result.nodes());
        assertTrue(result.links().isEmpty());
    }

    @Test
    void shouldBuildNetworkWhenCollaboratorsExist() throws IOException {
        var bucket = new LongTermsBucket.Builder().key(2L).docCount(3L).build();
        var termsAgg = new LongTermsAggregate.Builder()
            .buckets(new Buckets.Builder<LongTermsBucket>().array(List.of(bucket)).build())
            .build();

        var response = mock(SearchResponse.class);
        when(response.aggregations()).thenReturn(
            Map.of("coauthors", Aggregate.of(a -> a.lterms(termsAgg)))
        );
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Void.class)))
            .thenReturn(response);

        var p1 = new PersonIndex();
        p1.setDatabaseId(1);
        p1.setName("Author A");
        var p2 = new PersonIndex();
        p2.setDatabaseId(2);
        p2.setName("Author B");
        when(personIndexRepository.findByDatabaseIdIn(anyList(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(p1, p2)));

        var result = collaborationService.findCollaborationNetwork(1, 1);

        assertNotNull(result.nodes());
        assertNotNull(result.links());
    }
}
