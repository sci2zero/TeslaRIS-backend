package rs.teslaris.thesislibrary.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.functional.Triple;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;
import rs.teslaris.thesislibrary.service.impl.ThesisSearchServiceImpl;

@SpringBootTest
public class ThesisLibrarySearchServiceTest {

    @Mock
    private SearchService<DocumentPublicationIndex> searchService;

    @Mock
    private ExpressionTransformer expressionTransformer;

    @Mock
    private SearchFieldsLoader searchFieldsLoader;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private ThesisSearchServiceImpl thesisSearchService;

    private ThesisSearchRequestDTO searchRequest;

    private Pageable pageable;

    private List<DocumentPublicationIndex> documentList;


    @BeforeEach
    void setUp() {
        searchRequest = new ThesisSearchRequestDTO(
            List.of("test"), List.of(1), List.of(2), List.of(3),
            List.of(4), List.of(5), List.of(ThesisType.MASTER), false,
            LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)
        );
        pageable = mock(Pageable.class);
        documentList = List.of(mock(DocumentPublicationIndex.class));
    }

    @Test
    void shouldCallPerformQueryWhenPerformingSimpleThesisSearch() {
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(eq(1), eq(true)))
            .thenReturn(List.of(1, 10, 11));
        var expectedPage = new PageImpl<>(documentList);
        when(searchService.runQuery(any(), any(), eq(DocumentPublicationIndex.class), any()))
            .thenReturn(expectedPage);

        Page<DocumentPublicationIndex>
            result = thesisSearchService.performSimpleThesisSearch(searchRequest, pageable);

        assertNotNull(result);
        assertEquals(expectedPage, result);
        verify(searchService).runQuery(any(), any(), eq(DocumentPublicationIndex.class), any());
    }

    @Test
    void shouldCallPerformQueryWhenPerformingAdvancedThesisSearch() {
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(eq(1), eq(true)))
            .thenReturn(List.of(1, 10, 11));
        Page<DocumentPublicationIndex> expectedPage = new PageImpl<>(documentList);
        when(searchService.runQuery(any(), any(), eq(DocumentPublicationIndex.class), any()))
            .thenReturn(expectedPage);

        Page<DocumentPublicationIndex> result =
            thesisSearchService.performAdvancedThesisSearch(searchRequest, pageable);

        assertNotNull(result);
        assertEquals(expectedPage, result);
        verify(searchService).runQuery(any(), any(), eq(DocumentPublicationIndex.class), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnSearchFields(Boolean onlyExportFields) {
        // Given
        var expectedFields = List.of(
            new Triple<>("field1", List.of(new MultilingualContentDTO()), "Type1"),
            new Triple<>("field2", List.of(new MultilingualContentDTO()), "Type2")
        );

        when(searchFieldsLoader.getSearchFields(any(), anyBoolean())).thenReturn(expectedFields);

        // When
        var result = thesisSearchService.getSearchFields(onlyExportFields);

        // Then
        assertNotNull(result);
        assertEquals(expectedFields.size(), result.size());
    }

    @Test
    void shouldCallRunWordCloudSearchWhenPerformingSimpleThesisSearch() {
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(eq(1), eq(true)))
            .thenReturn(List.of(1, 10, 11));
        List<Pair<String, Long>> expectedResult = List.of(new Pair<>("ai", 42L));
        when(searchService.runWordCloudSearch(any(), any(), eq(false)))
            .thenReturn(expectedResult);

        var result =
            thesisSearchService.performWordCloudSearch(searchRequest, SearchRequestType.SIMPLE,
                false);

        assertNotNull(result);
        verify(searchService).runWordCloudSearch(any(), any(), anyBoolean());
    }

    private BoolQuery tokenQueryOf(List<String> tokens) {
        var query = thesisSearchService.buildSearchQuery(tokens, List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), null, null, SearchRequestType.SIMPLE, false);

        return query.bool().must().getFirst().bool().must().get(1).bool();
    }

    private Query wildcardOnField(BoolQuery tokenGroup, String field) {
        return tokenGroup.should().stream()
            .filter(clause -> clause.isWildcard() && clause.wildcard().field().equals(field))
            .findFirst()
            .orElseThrow();
    }

    @Test
    void shouldGroupClausesOfEachTokenIntoSingleShouldClause() {
        var tokenQuery = tokenQueryOf(List.of("prva", "druga"));

        assertEquals(2, tokenQuery.should().size());
        tokenQuery.should().forEach(clause -> assertTrue(clause.isBool()));
        assertEquals("2", tokenQuery.minimumShouldMatch());
    }

    @Test
    void shouldQueryKeywordFieldsWithSameValueForBothSerbianScripts() {
        var cyrillicGroup =
            tokenQueryOf(List.of("Библиотека")).should().getFirst().bool();
        var latinGroup = tokenQueryOf(List.of("Biblioteka")).should().getFirst().bool();

        assertEquals("*biblioteka*",
            wildcardOnField(cyrillicGroup, "keywords_sr").wildcard().value());
        assertEquals(wildcardOnField(cyrillicGroup, "keywords_sr").wildcard().value(),
            wildcardOnField(latinGroup, "keywords_sr").wildcard().value());
        assertEquals(wildcardOnField(cyrillicGroup, "keywords_other").wildcard().value(),
            wildcardOnField(latinGroup, "keywords_other").wildcard().value());
        assertEquals(wildcardOnField(cyrillicGroup, "title_sr").wildcard().value(),
            wildcardOnField(latinGroup, "title_sr").wildcard().value());
    }

    @Test
    void shouldSearchKeywordFieldsCaseInsensitively() {
        var tokenGroup = tokenQueryOf(List.of("Biblioteka")).should().getFirst().bool();

        assertTrue(wildcardOnField(tokenGroup, "keywords_sr").wildcard().caseInsensitive());
        assertTrue(wildcardOnField(tokenGroup, "keywords_other").wildcard().caseInsensitive());
        assertTrue(wildcardOnField(tokenGroup, "title_sr").wildcard().caseInsensitive());
        assertTrue(wildcardOnField(tokenGroup, "title_other").wildcard().caseInsensitive());
    }

    @Test
    void shouldStripQuotesFromPhraseTokens() {
        var tokenQuery = tokenQueryOf(List.of("\"neki tekst\""));

        var phraseClauses = tokenQuery.must().getFirst().bool().should();
        assertEquals(2, phraseClauses.size());
        phraseClauses.forEach(
            clause -> assertEquals("neki tekst", clause.matchPhrase().query()));

        assertEquals("neki tekst*",
            wildcardOnField(tokenQuery.should().getFirst().bool(), "title_sr").wildcard().value());
    }
}
