package rs.teslaris.core.unit.thesislibrary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;
import rs.teslaris.thesislibrary.service.impl.ThesisSearchServiceImpl;

@SpringBootTest
public class ThesisLibrarySearchServiceTest {

    @Mock
    private SearchService<DocumentPublicationIndex> searchService;

    @Mock
    private ExpressionTransformer expressionTransformer;

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
    void performSimpleThesisSearch_shouldCallPerformQuery() {
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
    void performAdvancedThesisSearch_shouldCallPerformQuery() {
        Page<DocumentPublicationIndex> expectedPage = new PageImpl<>(documentList);
        when(searchService.runQuery(any(), any(), eq(DocumentPublicationIndex.class), any()))
            .thenReturn(expectedPage);

        Page<DocumentPublicationIndex> result =
            thesisSearchService.performAdvancedThesisSearch(searchRequest, pageable);

        assertNotNull(result);
        assertEquals(expectedPage, result);
        verify(searchService).runQuery(any(), any(), eq(DocumentPublicationIndex.class), any());
    }
}
