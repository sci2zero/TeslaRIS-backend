package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.service.impl.document.PersonChartServiceImpl;

@SpringBootTest
public class PersonVisualizationDataServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private PersonChartServiceImpl service;


    @Test
    void shouldReturnBucketsWhenGettingPublicationCountsByTypeForAuthorAndYear()
        throws IOException {
        // Given
        var authorId = 42;
        var year = 2023;

        var mockBucket1 = mock(StringTermsBucket.class);
        when(mockBucket1.key()).thenReturn(FieldValue.of("JOURNAL"));
        when(mockBucket1.docCount()).thenReturn(5L);

        var mockBucket2 = mock(StringTermsBucket.class);
        when(mockBucket2.key()).thenReturn(FieldValue.of("MONOGRAPH"));
        when(mockBucket2.docCount()).thenReturn(2L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.sterms().buckets().array()).thenReturn(
            Arrays.asList(mockBucket1, mockBucket2));

        var mockResponse = mock(SearchResponse.class);
        var mockAggs = mock(HashMap.class);
        when(mockResponse.aggregations()).thenReturn(mockAggs);
        when(mockResponse.aggregations().get("by_type")).thenReturn(mockAgg);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            any())).thenReturn(mockResponse);

        // When
        var result = service.getPublicationCountsByTypeForAuthorAndYear(authorId, year);

        // Then
        var expected = new HashMap<String, Long>();
        expected.put("JOURNAL", 5L);
        expected.put("MONOGRAPH", 2L);

        assertEquals(expected, result);
    }

    @Test
    void shouldReturnYearlyCountsWhenGettingPublicationCountsForPerson() throws IOException {
        // Given
        var personId = 42;
        var startYear = 2020;
        var endYear = 2021;

        var mockCounts2020 = new HashMap<>();
        mockCounts2020.put("JOURNAL", 3L);

        var mockCounts2021 = new HashMap<String, Long>();
        mockCounts2021.put("MONOGRAPH", 1L);

        var serviceSpy = spy(service);
        doReturn(mockCounts2020).when(serviceSpy)
            .getPublicationCountsByTypeForAuthorAndYear(personId, 2020);
        doReturn(mockCounts2021).when(serviceSpy)
            .getPublicationCountsByTypeForAuthorAndYear(personId, 2021);

        // When
        var result = serviceSpy.getPublicationCountsForPerson(personId, startYear, endYear);

        // Then
        assertEquals(2, result.size());

        var yc2020 = result.getFirst();
        assertEquals(2020, yc2020.year());
        assertEquals(mockCounts2020, yc2020.countsByCategory());

        var yc2021 = result.get(1);
        assertEquals(2021, yc2021.year());
        assertEquals(mockCounts2021, yc2021.countsByCategory());
    }
}
