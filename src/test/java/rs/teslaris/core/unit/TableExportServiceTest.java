package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.commontypes.DocumentExportRequestDTO;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.service.impl.commontypes.TableExportServiceImpl;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;

@SpringBootTest
class TableExportServiceTest {

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private CitationService citationService;

    @InjectMocks
    private TableExportServiceImpl csvExportService;


    private DocumentExportRequestDTO request;
    private DocumentPublicationIndex mockDocument;


    @BeforeEach
    void setUp() {
        request = new DocumentExportRequestDTO();
        request.setColumns(List.of("title", "author"));
        request.setExportLanguage("en");
        request.setExportMaxPossibleAmount(false);
        request.setExportEntityIds(List.of(1, 2));

        mockDocument = new DocumentPublicationIndex();
        mockDocument.setDatabaseId(1);

        ReflectionTestUtils.setField(csvExportService, "maximumExportAmount", 500);
    }

    @ParameterizedTest
    @EnumSource(ExportFileType.class)
    void shouldExportCSVForSelectedDocuments(ExportFileType exportFileType) {
        // Given
        request.setExportFileType(exportFileType);

        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(1))
            .thenReturn(Optional.of(mockDocument));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(2))
            .thenReturn(Optional.empty());
        when(documentPublicationService.findOne(anyInt())).thenReturn(new Dataset() {{
            setId(1);
        }});

        // When
        var result = csvExportService.exportDocumentsToFile(request);

        // Then
        assertNotNull(result);
        verify(documentPublicationIndexRepository,
            atMostOnce()).findDocumentPublicationIndexByDatabaseId(1);
        verify(documentPublicationIndexRepository,
            atMostOnce()).findDocumentPublicationIndexByDatabaseId(2);
    }

    @ParameterizedTest
    @EnumSource(ExportFileType.class)
    void shouldExportCSVForMaxPossibleAmount(ExportFileType exportFileType) {
        // Given
        request.setExportMaxPossibleAmount(true);
        request.setBulkExportOffset(0);
        request.setExportFileType(exportFileType);
        var page = new PageImpl<>(List.of(mockDocument));

        when(documentPublicationIndexRepository.findAll(PageRequest.of(0, 500)))
            .thenReturn(page);
        when(documentPublicationService.findOne(anyInt())).thenReturn(new Dataset() {{
            setId(1);
        }});

        // When
        var result = csvExportService.exportDocumentsToFile(request);

        // Then
        assertNotNull(result);
        verify(documentPublicationIndexRepository, times(1))
            .findAll(PageRequest.of(0, 500));
    }

    @ParameterizedTest
    @EnumSource(ExportFileType.class)
    void shouldHandleEmptyDocumentsListGracefully(ExportFileType exportFileType) {
        // Given
        request.setExportFileType(exportFileType);
        request.setExportEntityIds(new ArrayList<>());

        // When
        var result = csvExportService.exportDocumentsToFile(request);

        // Then
        assertNotNull(result);
        verify(documentPublicationIndexRepository, never())
            .findDocumentPublicationIndexByDatabaseId(anyInt());
    }
}
