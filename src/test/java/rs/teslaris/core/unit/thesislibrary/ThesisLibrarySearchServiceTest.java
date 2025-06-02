package rs.teslaris.core.unit.thesislibrary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.Triple;
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
        List<Pair<String, Long>> expectedResult = List.of(new Pair<>("ai", 42L));
        when(searchService.runWordCloudSearch(any(), any(), eq(false)))
            .thenReturn(expectedResult);

        var result =
            thesisSearchService.performWordCloudSearch(searchRequest, SearchRequestType.SIMPLE,
                false);

        assertNotNull(result);
        verify(searchService).runWordCloudSearch(any(), any(), anyBoolean());
    }
}
