package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.commontypes.DocumentCSVExportRequest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.service.impl.commontypes.CSVExportServiceImpl;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.util.search.SearchFieldsLoader;

@SpringBootTest
class CSVExportServiceTest {

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private SearchFieldsLoader searchFieldsLoader;

    @Mock
    private CitationService citationService;

    @InjectMocks
    private CSVExportServiceImpl csvExportService;


    private DocumentCSVExportRequest request;
    private DocumentPublicationIndex mockDocument;


    @BeforeEach
    void setUp() {
        request = new DocumentCSVExportRequest();
        request.setColumns(List.of("title", "author"));
        request.setExportLanguage("en");
        request.setExportMaxPossibleAmount(false);
        request.setExportEntityIds(List.of(1, 2));

        mockDocument = new DocumentPublicationIndex();
        mockDocument.setDatabaseId(1);

        when(searchFieldsLoader.getSearchFieldLocalizedName(anyString(), anyString(), anyString()))
            .thenReturn("Localized Field");

        ReflectionTestUtils.setField(csvExportService, "maximumExportAmount", 500);
    }

    @Test
    void shouldExportCSVForSelectedDocuments() {
        // Given
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(1))
            .thenReturn(Optional.of(mockDocument));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(2))
            .thenReturn(Optional.empty());

        // When
        var result = csvExportService.exportDocumentsToCSV(request);

        // Then
        assertNotNull(result);
        verify(documentPublicationIndexRepository, times(1))
            .findDocumentPublicationIndexByDatabaseId(1);
        verify(documentPublicationIndexRepository, times(1))
            .findDocumentPublicationIndexByDatabaseId(2);
    }

    @Test
    void shouldExportCSVForMaxPossibleAmount() {
        // Given
        request.setExportMaxPossibleAmount(true);
        request.setBulkExportOffset(0);
        var page = new PageImpl<>(List.of(mockDocument));

        when(documentPublicationIndexRepository.findAll(PageRequest.of(0, 500)))
            .thenReturn(page);

        // When
        var result = csvExportService.exportDocumentsToCSV(request);

        // Then
        assertNotNull(result);
        verify(documentPublicationIndexRepository, times(1))
            .findAll(PageRequest.of(0, 500));
    }

    @Test
    void shouldHandleEmptyDocumentsListGracefully() {
        // Given
        request.setExportEntityIds(new ArrayList<>());

        // When
        var result = csvExportService.exportDocumentsToCSV(request);

        // Then
        assertNotNull(result);
        verify(documentPublicationIndexRepository, never())
            .findDocumentPublicationIndexByDatabaseId(anyInt());
    }
}
