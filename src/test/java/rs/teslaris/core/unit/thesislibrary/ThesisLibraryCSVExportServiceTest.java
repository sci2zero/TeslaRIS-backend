package rs.teslaris.core.unit.thesislibrary;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ExportableEndpointType;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.thesislibrary.dto.ThesisCSVExportRequestDTO;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;
import rs.teslaris.thesislibrary.service.impl.ThesisLibraryCSVExportServiceImpl;
import rs.teslaris.thesislibrary.service.interfaces.ThesisSearchService;

@SpringBootTest
public class ThesisLibraryCSVExportServiceTest {

    @Mock
    private ThesisSearchService thesisSearchService;

    @Mock
    private CitationService citationService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @InjectMocks
    private ThesisLibraryCSVExportServiceImpl thesisLibraryCSVExportService;

    private ThesisCSVExportRequestDTO request;
    private DocumentPublicationIndex mockDocument;


    @BeforeEach
    void setUp() {
        request = new ThesisCSVExportRequestDTO();
        request.setColumns(List.of("title", "author"));
        request.setExportLanguage("en");
        request.setExportEntityIds(List.of(1, 2));
        request.setThesisSearchRequest(
            new ThesisSearchRequestDTO(List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), false, null, null));
        request.setEndpointType(ExportableEndpointType.THESIS_SIMPLE_SEARCH);

        mockDocument = new DocumentPublicationIndex();
        mockDocument.setDatabaseId(1);

        ReflectionTestUtils.setField(thesisLibraryCSVExportService, "maximumExportAmount", 500);
    }

    @ParameterizedTest
    @EnumSource(ExportFileType.class)
    void shouldExportCSVForSelectedTheses(ExportFileType exportFileType) {
        // Given
        request.setExportFileType(exportFileType);
        request.setExportMaxPossibleAmount(false);

        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(1))
            .thenReturn(Optional.of(mockDocument));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(2))
            .thenReturn(Optional.empty());

        // When
        var result = thesisLibraryCSVExportService.exportThesesToCSV(request);

        // Then
        if (List.of(ExportFileType.CSV, ExportFileType.XLSX)
            .contains(request.getExportFileType())) {
            assertNotNull(result);
        } else {
            assertNull(result);
        }
        verify(documentPublicationIndexRepository, atMostOnce())
            .findDocumentPublicationIndexByDatabaseId(1);
        verify(documentPublicationIndexRepository, atMostOnce())
            .findDocumentPublicationIndexByDatabaseId(2);
    }

    @ParameterizedTest
    @EnumSource(ExportFileType.class)
    void shouldExportCSVForMaxPossibleTheses(ExportFileType exportFileType) {
        // Given
        request.setExportFileType(exportFileType);
        request.setExportMaxPossibleAmount(true);
        request.setBulkExportOffset(0);

        var page = new PageImpl<>(List.of(mockDocument));
        when(thesisSearchService.performSimpleThesisSearch(any(), any()))
            .thenReturn(page);

        // When
        var result = thesisLibraryCSVExportService.exportThesesToCSV(request);

        // Then
        if (List.of(ExportFileType.CSV, ExportFileType.XLSX)
            .contains(request.getExportFileType())) {
            assertNotNull(result);
        } else {
            assertNull(result);
        }
        verify(thesisSearchService, atMostOnce())
            .performSimpleThesisSearch(any(), any());
    }

    @ParameterizedTest
    @EnumSource(ExportFileType.class)
    void shouldHandleAdvancedSearchEndpoint(ExportFileType exportFileType) {
        // Given
        request.setExportFileType(exportFileType);
        request.setExportMaxPossibleAmount(true);
        request.setBulkExportOffset(0);
        request.setEndpointType(ExportableEndpointType.THESIS_ADVANCED_SEARCH);

        var page = new PageImpl<>(List.of(mockDocument));
        when(thesisSearchService.performAdvancedThesisSearch(any(), any()))
            .thenReturn(page);

        // When
        var result = thesisLibraryCSVExportService.exportThesesToCSV(request);

        // Then
        if (List.of(ExportFileType.CSV, ExportFileType.XLSX)
            .contains(request.getExportFileType())) {
            assertNotNull(result);
        } else {
            assertNull(result);
        }
        verify(thesisSearchService, atMostOnce())
            .performAdvancedThesisSearch(any(), any());
    }

    @ParameterizedTest
    @EnumSource(ExportFileType.class)
    void shouldHandleEmptyThesisListGracefully(ExportFileType exportFileType) {
        // Given
        request.setExportFileType(exportFileType);
        request.setExportEntityIds(Collections.emptyList());
        request.setExportMaxPossibleAmount(false);

        // When
        var result = thesisLibraryCSVExportService.exportThesesToCSV(request);

        // Then
        if (List.of(ExportFileType.CSV, ExportFileType.XLSX)
            .contains(request.getExportFileType())) {
            assertNotNull(result);
        } else {
            assertNull(result);
        }
        verify(documentPublicationIndexRepository, never())
            .findDocumentPublicationIndexByDatabaseId(anyInt());
    }
}
