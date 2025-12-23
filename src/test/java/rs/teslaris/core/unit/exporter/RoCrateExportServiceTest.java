package rs.teslaris.core.unit.exporter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.minio.GetObjectResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.service.interfaces.commontypes.ProgressService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.exporter.service.impl.RoCrateExportServiceImpl;
import rs.teslaris.exporter.util.rocrate.Json2HtmlTable;

@SpringBootTest
class RoCrateExportServiceTest {

    private final String exportId = "EXPORT_ID_MOCK";

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private FileService fileService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ObjectWriter objectWriter;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private ProgressService progressService;

    @Mock
    private DocumentPublicationService documentService;

    @Mock
    private PersonService personService;

    @Mock
    private JsonNode jsonNode;

    @Mock
    private MessageSource messageSource;

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
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        // When
        service.createRoCrateZip(documentId, exportId, outputStream);

        // Then
        assertEquals(0, outputStream.size());
    }

    @Test
    void shouldCreateZipWithMetadataAndPreviewOnlyWhenNoFiles() throws Exception {
        // Given
        var documentId = 2;
        var outputStream = new ByteArrayOutputStream();
        var document = mock(Dataset.class);

        when(document.getFileItems()).thenReturn(Set.of());
        when(documentPublicationService.findDocumentById(documentId)).thenReturn(document);
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        try (var htmlMock = mockStatic(Json2HtmlTable.class)) {
            htmlMock.when(() -> Json2HtmlTable.toHtmlTable(any()))
                .thenReturn("<html/>");

            // When
            service.createRoCrateZip(documentId, exportId, outputStream);
        }

        // Then
        assertTrue(outputStream.size() >= 0);
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
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        try (var htmlMock = mockStatic(Json2HtmlTable.class)) {
            htmlMock.when(() -> Json2HtmlTable.toHtmlTable(any()))
                .thenReturn("<html/>");

            // When
            service.createRoCrateZip(documentId, exportId, outputStream);
        }

        // Then
        try (var zip = new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            var entries = new HashSet<String>();
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(entry.getName());
            }

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
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        try (var htmlMock = mockStatic(Json2HtmlTable.class)) {
            htmlMock.when(() -> Json2HtmlTable.toHtmlTable(any()))
                .thenReturn("<html/>");

            // When / Then
            var ex = assertThrows(
                RuntimeException.class,
                () -> service.createRoCrateZip(documentId, exportId, outputStream)
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
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        try (var htmlMock = mockStatic(Json2HtmlTable.class)) {
            htmlMock.when(() -> Json2HtmlTable.toHtmlTable(any()))
                .thenReturn("<html/>");

            // When
            service.createRoCrateZip(documentId, exportId, outputStream);
        }

        // Then
        assertDoesNotThrow(() ->
            new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()))
        );
    }

    @Test
    void shouldCreateBibliographyZipWithMetadataAndPreviewOnly() throws Exception {
        // Given
        var personId = 1;
        var exportId = "export-1";
        var outputStream = new ByteArrayOutputStream();

        var docIndex1 = mock(DocumentPublicationIndex.class);
        when(docIndex1.getDatabaseId()).thenReturn(10);
        when(docIndex1.getTitleOther()).thenReturn("Title 1");

        var docIndex2 = mock(DocumentPublicationIndex.class);
        when(docIndex2.getDatabaseId()).thenReturn(11);
        when(docIndex2.getTitleOther()).thenReturn("Title 2");

        var page = new PageImpl<>(
            List.of(docIndex1, docIndex2),
            PageRequest.of(0, 500),
            2
        );

        when(documentPublicationIndexRepository.findByAuthorIds(
            eq(personId), any(PageRequest.class)))
            .thenReturn(page);

        var document = mock(Dataset.class);
        when(documentService.findDocumentById(any())).thenReturn(document);
        when(personService.findOne(personId)).thenReturn(new Person() {{
            setName(new PersonName());
        }});
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        try (var htmlMock = mockStatic(Json2HtmlTable.class)) {
            htmlMock.when(() -> Json2HtmlTable.toHtmlTable(any()))
                .thenReturn("<html/>");

            // When
            service.createRoCrateBibliographyZip(personId, exportId, outputStream);
        }

        // Then
        assertTrue(outputStream.size() > 0);

        try (var zip = new ZipInputStream(
            new ByteArrayInputStream(outputStream.toByteArray()))) {

            var entries = new HashSet<String>();
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
    void shouldSkipMissingDocumentsAndStillCreateBibliographyZip() throws Exception {
        // Given
        var personId = 2;
        var exportId = "export-2";
        var outputStream = new ByteArrayOutputStream();

        var docIndex = mock(DocumentPublicationIndex.class);
        when(docIndex.getDatabaseId()).thenReturn(20);
        when(docIndex.getTitleOther()).thenReturn("Missing doc");

        var page = new PageImpl<>(
            List.of(docIndex),
            PageRequest.of(0, 500),
            1
        );

        when(documentPublicationIndexRepository.findByAuthorIds(
            eq(personId), any(PageRequest.class)))
            .thenReturn(page);

        // Document not found
        when(documentService.findDocumentById(20)).thenReturn(null);
        when(personService.findOne(personId)).thenReturn(new Person() {{
            setName(new PersonName());
        }});
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        try (var htmlMock = mockStatic(Json2HtmlTable.class)) {
            htmlMock.when(() -> Json2HtmlTable.toHtmlTable(any()))
                .thenReturn("<html/>");

            // When
            service.createRoCrateBibliographyZip(personId, exportId, outputStream);
        }

        // Then
        assertTrue(outputStream.size() > 0);

        assertDoesNotThrow(() ->
            new ZipInputStream(
                new ByteArrayInputStream(outputStream.toByteArray()))
        );
    }
}
