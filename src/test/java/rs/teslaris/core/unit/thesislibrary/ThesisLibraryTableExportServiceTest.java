package rs.teslaris.core.unit.thesislibrary;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.converter.document.DocumentPublicationConverter;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ExportableEndpointType;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;
import rs.teslaris.thesislibrary.dto.ThesisTableExportRequestDTO;
import rs.teslaris.thesislibrary.service.impl.ThesisLibraryTableExportServiceImpl;
import rs.teslaris.thesislibrary.service.interfaces.ThesisSearchService;

@SpringBootTest
public class ThesisLibraryTableExportServiceTest {

    @Mock
    private ThesisSearchService thesisSearchService;

    @Mock
    private CitationService citationService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private ThesisService thesisService;

    @InjectMocks
    private ThesisLibraryTableExportServiceImpl thesisLibraryCSVExportService;

    private ThesisTableExportRequestDTO request;
    private DocumentPublicationIndex mockDocument;


    @BeforeEach
    void setUp() {
        request = new ThesisTableExportRequestDTO();
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

    @ParameterizedTest
    @EnumSource(value = ExportFileType.class, names = {"BIB", "RIS", "ENW"})
    void shouldExportBibliographicFileForSelectedTheses(ExportFileType exportFileType) {
        // Given
        request.setExportFileType(exportFileType);
        request.setExportMaxPossibleAmount(false);

        Thesis thesis1 = new Thesis();
        Thesis thesis2 = new Thesis();

        when(thesisService.getThesisById(1)).thenReturn(thesis1);
        when(thesisService.getThesisById(2)).thenReturn(thesis2);

        // mock conversion
        try (MockedStatic<DocumentPublicationConverter> mockedConverter = mockStatic(
            DocumentPublicationConverter.class)) {
            mockedConverter.when(() -> DocumentPublicationConverter
                    .getBibliographicExportEntity(eq(request), eq(thesis1)))
                .thenReturn("thesis1-bib");
            mockedConverter.when(() -> DocumentPublicationConverter
                    .getBibliographicExportEntity(eq(request), eq(thesis2)))
                .thenReturn("thesis2-bib");

            // When
            var result = thesisLibraryCSVExportService.exportThesesToBibliographicFile(request);

            // Then
            assertNotNull(result);
            verify(thesisService).getThesisById(1);
            verify(thesisService).getThesisById(2);
        }
    }

    @ParameterizedTest
    @EnumSource(value = ExportFileType.class, mode = EnumSource.Mode.EXCLUDE, names = {"BIB", "RIS",
        "ENW"})
    void shouldReturnNullForNonBibliographicFormats(ExportFileType exportFileType) {
        // Given
        request.setExportFileType(exportFileType);

        // When
        var result = thesisLibraryCSVExportService.exportThesesToBibliographicFile(request);

        // Then
        assertNull(result);
        verifyNoInteractions(thesisService);
    }

    @ParameterizedTest
    @EnumSource(value = ExportFileType.class, names = {"BIB", "RIS", "ENW"})
    void shouldExportBibliographicFileWhenExportMaxPossibleAmountTrue(
        ExportFileType exportFileType) {
        // Given
        request.setExportFileType(exportFileType);
        request.setExportMaxPossibleAmount(true);
        request.setBulkExportOffset(600);
        request.setEndpointType(ExportableEndpointType.THESIS_ADVANCED_SEARCH);

        var thesis1 = new Thesis();
        var entityIndex = new DocumentPublicationIndex();
        entityIndex.setDatabaseId(1);

        try (MockedStatic<DocumentPublicationConverter> mockedConverter = mockStatic(
            DocumentPublicationConverter.class)) {
            when(thesisSearchService.performAdvancedThesisSearch(any(), any()))
                .thenReturn(new PageImpl<>(List.of(entityIndex)));

            when(thesisService.getThesisById(1)).thenReturn(thesis1);

            mockedConverter.when(() -> DocumentPublicationConverter
                    .getBibliographicExportEntity(eq(request), eq(thesis1)))
                .thenReturn("thesis1-bib");

            // When
            var result = thesisLibraryCSVExportService.exportThesesToBibliographicFile(request);

            // Then
            assertNotNull(result);
            verify(thesisService).getThesisById(1);
        }
    }
}
