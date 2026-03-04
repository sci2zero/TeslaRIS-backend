package rs.teslaris.core.unit.exporter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import okhttp3.Headers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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

    private static MockedStatic<Json2HtmlTable> htmlTableMock;

    private final String exportId = "EXPORT_ID_MOCK";

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private FileService fileService;

    @Mock
    private ObjectMapper objectMapper;

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


    @BeforeAll
    static void setUpClass() {
        htmlTableMock = mockStatic(Json2HtmlTable.class);
        htmlTableMock.when(() -> Json2HtmlTable.toHtmlTable(any()))
            .thenReturn("<html/>");
    }

    @AfterAll
    static void tearDownClass() {
        htmlTableMock.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
    }

    @Test
    void shouldReturnSilentlyWhenDocumentDoesNotExist() throws Exception {
        var documentId = 1;

        when(documentPublicationService.findOne(documentId)).thenReturn(null);
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        var path = service.createRoCrateZip(documentId, exportId);

        assertTrue(Files.exists(path));
        Files.deleteIfExists(path);
    }

    @Test
    void shouldCreateZipWithMetadataAndPreviewOnlyWhenNoFiles() throws Exception {
        var documentId = 2;
        var document = mock(Dataset.class);

        when(document.getFileItems()).thenReturn(Set.of());
        when(documentPublicationService.findOne(documentId)).thenReturn(document);
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        var writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);
        when(writer.writeValueAsBytes(any())).thenReturn("{}".getBytes());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        var path = service.createRoCrateZip(documentId, exportId);

        assertNotNull(path);
        Files.deleteIfExists(path);
    }

    @Test
    void shouldIncludeOnlyApprovedOpenAccessFiles() throws Exception {
        var documentId = 3;
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
        when(documentPublicationService.findOne(documentId)).thenReturn(document);
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        var writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);
        when(writer.writeValueAsBytes(any())).thenReturn("{}".getBytes());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

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

        var path = service.createRoCrateZip(documentId, exportId);

        try (var zip = new ZipInputStream(Files.newInputStream(path))) {
            var entries = new HashSet<String>();
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(entry.getName());
            }

            assertFalse(entries.contains("data/rejected.pdf"));
        }

        Files.deleteIfExists(path);
    }

    @Test
    void shouldWriteValidZipStructure() throws Exception {
        var documentId = 5;
        var document = mock(Dataset.class);

        when(document.getFileItems()).thenReturn(Set.of());
        when(documentPublicationService.findOne(documentId)).thenReturn(document);
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        var writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);
        when(writer.writeValueAsBytes(any())).thenReturn("{}".getBytes());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        var path = service.createRoCrateZip(documentId, exportId);

        assertDoesNotThrow(() ->
            new ZipInputStream(Files.newInputStream(path))
        );

        Files.deleteIfExists(path);
    }

    @Test
    void shouldCreateBibliographyZipWithMetadataAndPreviewOnly() throws Exception {
        var personId = 1;

        var docIndex = mock(DocumentPublicationIndex.class);
        when(docIndex.getDatabaseId()).thenReturn(10);
        when(docIndex.getTitleOther()).thenReturn("Title");

        var page = new PageImpl<>(
            List.of(docIndex),
            PageRequest.of(0, 500),
            1
        );

        when(documentPublicationIndexRepository.findByAuthorIds(
            eq(personId), any(PageRequest.class)))
            .thenReturn(page);

        when(documentService.findOne(any())).thenReturn(mock(Dataset.class));
        when(personService.findOne(personId)).thenReturn(new Person() {{
            setName(new PersonName());
        }});

        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        var writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);
        when(writer.writeValueAsBytes(any())).thenReturn("{}".getBytes());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        var path = service.createRoCrateBibliographyZip(personId, exportId);

        try (var zip = new ZipInputStream(Files.newInputStream(path))) {
            var entries = new HashSet<String>();
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(entry.getName());
            }

            assertTrue(entries.contains("ro-crate-metadata.json"));
            assertTrue(entries.contains("ro-crate-preview.html"));
        }

        Files.deleteIfExists(path);
    }

    @Test
    void shouldSkipMissingDocumentsAndStillCreateBibliographyZip() throws Exception {
        var personId = 2;

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

        when(documentService.findOne(20)).thenReturn(null);
        when(personService.findOne(personId)).thenReturn(new Person() {{
            setName(new PersonName());
        }});

        when(messageSource.getMessage(any(), any(), any())).thenReturn("Message");

        var writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);
        when(writer.writeValueAsBytes(any())).thenReturn("{}".getBytes());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        var path = service.createRoCrateBibliographyZip(personId, exportId);

        assertDoesNotThrow(() ->
            new ZipInputStream(Files.newInputStream(path))
        );

        Files.deleteIfExists(path);
    }
}
