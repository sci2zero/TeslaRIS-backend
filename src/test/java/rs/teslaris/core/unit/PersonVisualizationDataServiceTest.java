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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.impl.document.PersonVisualizationDataServiceImpl;
import rs.teslaris.core.service.interfaces.person.PersonService;

@SpringBootTest
public class PersonVisualizationDataServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private PersonService personService;

    @InjectMocks
    private PersonVisualizationDataServiceImpl service;


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
        var result = service.getPublicationCountsForPerson(authorId, year, year);

        // Then
        var expected = new HashMap<String, Long>();
        expected.put("JOURNAL", 5L);
        expected.put("MONOGRAPH", 2L);

        assertEquals(expected, result.getFirst().countsByCategory());
    }

    @Test
    void shouldReturnYearlyCountsWhenGettingPublicationCountsForPerson() throws IOException {
        // Given
        var personId = 42;
        var startYear = 2020;
        var endYear = 2021;

        var mockCounts2020 = new ArrayList<PersonVisualizationDataServiceImpl.YearlyCounts>();
        mockCounts2020.add(
            new PersonVisualizationDataServiceImpl.YearlyCounts(2020, Map.of("JOURNAL", 3L)));

        var mockCounts2021 = new ArrayList<PersonVisualizationDataServiceImpl.YearlyCounts>();
        mockCounts2021.add(
            new PersonVisualizationDataServiceImpl.YearlyCounts(2021, Map.of("MONOGRAPH", 1L)));

        var serviceSpy = spy(service);
        mockCounts2020.addAll(mockCounts2021);
        doReturn(mockCounts2020).when(serviceSpy)
            .getPublicationCountsForPerson(personId, 2020, 2021);

        // When
        var result = serviceSpy.getPublicationCountsForPerson(personId, startYear, endYear);

        // Then
        assertEquals(2, result.size());

        var yc2020 = result.getFirst();
        assertEquals(2020, yc2020.year());
        assertEquals(mockCounts2020.getFirst().countsByCategory(), yc2020.countsByCategory());

        var yc2021 = result.get(1);
        assertEquals(2021, yc2021.year());
        assertEquals(mockCounts2021.getLast().countsByCategory(), yc2021.countsByCategory());
    }

    @Test
    void shouldReturnCountryBucketsWhenGettingStatisticsByCountryForPerson() throws IOException {
        // Given
        var personId = 123;
        var statisticsType = StatisticsType.VIEW;

        var person = mock(Person.class);
        when(person.getMergedIds()).thenReturn(Set.of(456, 789));
        when(personService.findOne(personId)).thenReturn(person);

        // Mock bucket 1 (US)
        var mockBucket1 = mock(StringTermsBucket.class);
        when(mockBucket1.key()).thenReturn(FieldValue.of("US"));
        when(mockBucket1.docCount()).thenReturn(10L);

        var mockAggs = mock(HashMap.class);

        var mockSubBucket1 = mock(StringTermsBucket.class);
        when(mockSubBucket1.key()).thenReturn(FieldValue.of("United States"));
        var mockSubAgg1 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockSubAgg1.sterms().buckets().array()).thenReturn(List.of(mockSubBucket1));
        when(mockBucket1.aggregations()).thenReturn(mockAggs);
        when(mockBucket1.aggregations().get("country_name")).thenReturn(mockSubAgg1);

        // Mock bucket 2 (DE)
        var mockBucket2 = mock(StringTermsBucket.class);
        when(mockBucket2.key()).thenReturn(FieldValue.of("DE"));
        when(mockBucket2.docCount()).thenReturn(7L);

        var mockSubBucket2 = mock(StringTermsBucket.class);
        when(mockSubBucket2.key()).thenReturn(FieldValue.of("Germany"));
        var mockSubAgg2 = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockSubAgg2.sterms().buckets().array()).thenReturn(List.of(mockSubBucket2));
        when(mockBucket2.aggregations()).thenReturn(mockAggs);
        when(mockBucket2.aggregations().get("country_name")).thenReturn(mockSubAgg2);

        // Mock top-level aggregation
        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.sterms().buckets().array()).thenReturn(List.of(mockBucket1, mockBucket2));

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(mockAggs);
        when(mockResponse.aggregations().get("by_country")).thenReturn(mockAgg);

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            any()
        )).thenReturn(mockResponse);

        // When
        var result = service.getByCountryStatisticsForPerson(personId, statisticsType);

        // Then
        assertEquals(2, result.size());

        var first = result.getFirst();
        assertEquals("US", first.countryCode());
        assertEquals(10L, first.value());

        var second = result.get(1);
        assertEquals("DE", second.countryCode());
        assertEquals(7L, second.value());
    }
}
