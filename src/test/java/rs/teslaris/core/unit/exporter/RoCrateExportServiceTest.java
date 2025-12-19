package rs.teslaris.core.unit.exporter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.minio.GetObjectResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.exporter.service.impl.RoCrateExportServiceImpl;
import rs.teslaris.exporter.util.rocrate.Json2HtmlTable;

@SpringBootTest
class RoCrateExportServiceTest {

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private FileService fileService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ObjectWriter objectWriter;

    @Mock
    private JsonNode jsonNode;

    @InjectMocks
    private RoCrateExportServiceImpl service;


    @BeforeEach
    void setUp() throws Exception {
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsBytes(any())).thenReturn("{}".getBytes());
        when(objectWriter.writeValueAsString(any())).thenReturn("{}");
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
    }

    @Test
    void shouldReturnSilentlyWhenDocumentDoesNotExist() {
        // Given
        var documentId = 1;
        var outputStream = new ByteArrayOutputStream();
        when(documentPublicationService.findDocumentById(documentId)).thenReturn(null);

        // When
        service.createRoCrateZip(documentId, outputStream);

        // Then
        assertEquals(0, outputStream.size());
        verify(documentPublicationService).findDocumentById(documentId);
    }

    @Test
    void shouldCreateZipWithMetadataAndPreviewOnlyWhenNoFiles() throws Exception {
        // Given
        var documentId = 2;
        var outputStream = new ByteArrayOutputStream();
        var document = mock(Dataset.class);

        when(document.getFileItems()).thenReturn(Set.of());
        when(documentPublicationService.findDocumentById(documentId)).thenReturn(document);

        try (var htmlMock = mockStatic(Json2HtmlTable.class)) {
            htmlMock.when(() -> Json2HtmlTable.toHtmlTable(any()))
                .thenReturn("<html/>");

            // When
            service.createRoCrateZip(documentId, outputStream);
        }

        // Then
        assertTrue(outputStream.size() > 0);

        try (var zip = new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            var entries = new HashSet<>();
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(entry.getName());
            }

            assertTrue(entries.contains("ro-crate-metadata.json"));
            assertTrue(entries.contains("ro-crate-preview.html"));
            assertEquals(2, entries.size());
        }
    }

    @Test
    void shouldIncludeOnlyApprovedOpenAccessFiles() throws Exception {
        // Given
        var documentId = 3;
        var outputStream = new ByteArrayOutputStream();
        var document = mock(Dataset.class);

        var approvedFile = mock(DocumentFile.class);
        when(approvedFile.getApproveStatus()).thenReturn(ApproveStatus.APPROVED);
        when(approvedFile.getAccessRights()).thenReturn(AccessRights.OPEN_ACCESS);
        when(approvedFile.getLicense()).thenReturn(License.BY_NC);
        when(approvedFile.getFilename()).thenReturn("file.pdf");
        when(approvedFile.getServerFilename()).thenReturn("server-file.pdf");

        var rejectedFile = mock(DocumentFile.class);
        when(rejectedFile.getApproveStatus()).thenReturn(ApproveStatus.DECLINED);
        when(rejectedFile.getAccessRights()).thenReturn(AccessRights.OPEN_ACCESS);
        when(rejectedFile.getLicense()).thenReturn(License.BY);

        when(document.getFileItems()).thenReturn(Set.of(approvedFile, rejectedFile));
        when(documentPublicationService.findDocumentById(documentId)).thenReturn(document);

        // Given
        var body = new ByteArrayInputStream("data".getBytes());

        var headers = Headers.of("Content-Length", "4");

        var response = new GetObjectResponse(
            headers,
            "test-bucket",
            "us-east-1",
            "server-file.pdf",
            body
        );

        when(fileService.loadAsResource(any())).thenReturn(response);

        try (var htmlMock = mockStatic(Json2HtmlTable.class)) {
            htmlMock.when(() -> Json2HtmlTable.toHtmlTable(any()))
                .thenReturn("<html/>");

            // When
            service.createRoCrateZip(documentId, outputStream);
        }

        // Then
        try (var zip = new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            var entries = new HashSet<String>();
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(entry.getName());
            }

            assertTrue(entries.contains("data/file.pdf"));
            assertFalse(entries.contains("data/rejected.pdf"));
        }
    }

    @Test
    void shouldThrowLoadingExceptionWhenObjectMapperFails() throws Exception {
        // Given
        var documentId = 4;
        var outputStream = new ByteArrayOutputStream();
        var document = mock(Dataset.class);

        when(document.getFileItems()).thenReturn(Set.of());
        when(documentPublicationService.findDocumentById(documentId)).thenReturn(document);
        when(objectWriter.writeValueAsBytes(any())).thenThrow(new RuntimeException("boom"));

        try (var htmlMock = mockStatic(Json2HtmlTable.class)) {
            htmlMock.when(() -> Json2HtmlTable.toHtmlTable(any()))
                .thenReturn("<html/>");

            // When / Then
            var ex = assertThrows(
                RuntimeException.class,
                () -> service.createRoCrateZip(documentId, outputStream)
            );

            assertTrue(ex.getMessage().contains("Failed to create RO-Crate ZIP"));
        }
    }

    @Test
    void shouldWriteValidZipStructure() {
        // Given
        var documentId = 5;
        var outputStream = new ByteArrayOutputStream();
        var document = mock(Dataset.class);

        when(document.getFileItems()).thenReturn(Set.of());
        when(documentPublicationService.findDocumentById(documentId)).thenReturn(document);

        try (var htmlMock = mockStatic(Json2HtmlTable.class)) {
            htmlMock.when(() -> Json2HtmlTable.toHtmlTable(any()))
                .thenReturn("<html/>");

            // When
            service.createRoCrateZip(documentId, outputStream);
        }

        // Then
        assertDoesNotThrow(() ->
            new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()))
        );
    }
}
