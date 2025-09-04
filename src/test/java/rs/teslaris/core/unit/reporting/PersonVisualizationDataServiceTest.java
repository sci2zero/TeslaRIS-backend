package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.reporting.dto.YearlyCounts;
import rs.teslaris.reporting.service.impl.PersonVisualizationDataServiceImpl;

@SpringBootTest
public class PersonVisualizationDataServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private PersonService personService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvolvementRepository involvementRepository;

    @InjectMocks
    private PersonVisualizationDataServiceImpl service;


    @Test
    @SuppressWarnings("unchecked")
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

        var mockCounts2020 = new ArrayList<YearlyCounts>();
        mockCounts2020.add(new YearlyCounts(2020, Map.of("JOURNAL", 3L)));

        var mockCounts2021 = new ArrayList<YearlyCounts>();
        mockCounts2021.add(new YearlyCounts(2021, Map.of("MONOGRAPH", 1L)));

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
    @SuppressWarnings("unchecked")
    void shouldReturnCountryBucketsWhenGettingStatisticsByCountryForPerson() throws IOException {
        // Given
        var personId = 123;

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
        var result =
            service.getByCountryStatisticsForPerson(personId, LocalDate.now().minusMonths(12),
                LocalDate.now());

        // Then
        assertEquals(2, result.size());

        var first = result.getFirst();
        assertEquals("US", first.countryCode());
        assertEquals(10L, first.value());

        var second = result.get(1);
        assertEquals("DE", second.countryCode());
        assertEquals(7L, second.value());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnCommissionYearlyCountsForPerson() throws IOException {
        // Given
        var personId = 123;
        var startYear = 2020;
        var endYear = 2021;

        when(involvementRepository.findActiveEmploymentInstitutionIds(personId)).thenReturn(
            List.of(1));
        when(userRepository.findUserCommissionForOrganisationUnit(1)).thenReturn(
            List.of(new Commission()));

        // Mock ES aggregation buckets
        var mockBucket1 = mock(StringTermsBucket.class);
        when(mockBucket1.key()).thenReturn(FieldValue.of("M50"));
        when(mockBucket1.docCount()).thenReturn(5L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.sterms().buckets().array()).thenReturn(List.of(mockBucket1));

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(Map.of("by_m_category", mockAgg));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenReturn(
            mockResponse);

        // When
        var result = service.getMCategoryCountsForPerson(personId, startYear, endYear);

        // Then
        assertEquals(1, result.size());
        var commissionCounts = result.getFirst();
        assertTrue(commissionCounts.yearlyCounts().stream()
            .allMatch(yc -> yc.countsByCategory().containsKey("M50")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnMonthlyStatisticsCountsForPerson() throws IOException {
        // Given
        var personId = 123;
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 3, 1);

        when(personService.findOne(personId)).thenReturn(new Person() {{
            setMergedIds(new HashSet<>(List.of(2, 3)));
        }});

        var mockBucket1 = mock(DateHistogramBucket.class);
        when(mockBucket1.keyAsString()).thenReturn("2025-01");
        when(mockBucket1.docCount()).thenReturn(10L);

        var mockBucket2 = mock(DateHistogramBucket.class);
        when(mockBucket2.keyAsString()).thenReturn("2025-02");
        when(mockBucket2.docCount()).thenReturn(7L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.dateHistogram().buckets().array()).thenReturn(
            List.of(mockBucket1, mockBucket2));

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(Map.of("per_month", mockAgg));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenReturn(
            mockResponse);

        // When
        var result = service.getMonthlyStatisticsCounts(personId, from, to);

        // Then
        assertEquals(2, result.size());
        assertEquals(10L, result.get(YearMonth.of(2025, 1)));
        assertEquals(7L, result.get(YearMonth.of(2025, 2)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnMCategoriesForPersonPublications() throws IOException {
        // Given
        var personId = 123;
        var startYear = 2020;
        var endYear = 2020;

        when(involvementRepository.findActiveEmploymentInstitutionIds(personId)).thenReturn(
            List.of(1));
        when(userRepository.findUserCommissionForOrganisationUnit(1)).thenReturn(
            List.of(new Commission()));

        // Mock ES aggregation buckets
        var mockBucket1 = mock(StringTermsBucket.class);
        when(mockBucket1.key()).thenReturn(FieldValue.of("M50"));
        when(mockBucket1.docCount()).thenReturn(5L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.sterms().buckets().array()).thenReturn(List.of(mockBucket1));

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(Map.of("by_m_category", mockAgg));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenReturn(
            mockResponse);

        // When
        var result = service.getPersonPublicationsByMCategories(personId, startYear, endYear);

        // Then
        assertEquals(1, result.size());
        var mCounts = result.getFirst();
        assertEquals(1, mCounts.countsByCategory().size());
        assertEquals(5L, mCounts.countsByCategory().get("M50"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnYearlyStatisticsCountsForPerson() throws IOException {
        // Given
        var personId = 123;
        var startYear = 2024;
        var endYear = 2025;

        when(personService.findOne(personId)).thenReturn(new Person() {{
            setMergedIds(new HashSet<>(List.of(2, 3)));
        }});

        var mockBucket1 = mock(DateHistogramBucket.class);
        when(mockBucket1.keyAsString()).thenReturn("2024");
        when(mockBucket1.docCount()).thenReturn(15L);

        var mockBucket2 = mock(DateHistogramBucket.class);
        when(mockBucket2.keyAsString()).thenReturn("2025");
        when(mockBucket2.docCount()).thenReturn(20L);

        var mockAgg = mock(Aggregate.class, RETURNS_DEEP_STUBS);
        when(mockAgg.dateHistogram().buckets().array()).thenReturn(
            List.of(mockBucket1, mockBucket2)
        );

        var mockResponse = mock(SearchResponse.class);
        when(mockResponse.aggregations()).thenReturn(Map.of("per_year", mockAgg));

        when(elasticsearchClient.search(
            (Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>) any(),
            eq(Void.class))).thenReturn(
            mockResponse
        );

        // When
        var result = service.getYearlyStatisticsCounts(personId, startYear, endYear);

        // Then
        assertEquals(2, result.size());
        assertEquals(15L, result.get(Year.of(2024)));
        assertEquals(20L, result.get(Year.of(2025)));
    }
}
