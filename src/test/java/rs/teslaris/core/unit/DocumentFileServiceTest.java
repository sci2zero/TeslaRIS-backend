package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.GetObjectResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.indexrepository.DocumentFileIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.repository.document.DocumentFileRepository;
import rs.teslaris.core.service.impl.document.DocumentFileServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchRequestType;

@SpringBootTest
public class DocumentFileServiceTest {

    @Mock
    private FileService fileService;

    @Mock
    private DocumentFileRepository documentFileRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private DocumentFileIndexRepository documentFileIndexRepository;

    @Mock
    private SearchService<DocumentFileIndex> searchService;

    @Mock
    private ExpressionTransformer expressionTransformer;

    @InjectMocks
    private DocumentFileServiceImpl documentFileService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(documentFileService, "documentFileApprovedByDefault", true);
    }

    @Test
    public void shouldReturnDocumentFileWhenValidIdIsProvided() {
        // given
        var documentFile = new DocumentFile();

        when(documentFileRepository.findById(1)).thenReturn(Optional.of(documentFile));

        // when
        var actual = documentFileService.findOne(1);

        // then
        assertEquals(documentFile, actual);
    }

    @Test
    public void shouldThrowExceptionWhenInvalidIdIsProvided() {
        // given
        when(documentFileRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> documentFileService.findOne(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldSaveNewDocumentWhenValidDataIsProvided() {
        // given
        var dto = new DocumentFileDTO();
        var doc = new DocumentFile();
        dto.setAccessRights(AccessRights.OPEN_ACCESS);
        dto.setLicense(License.BY_NC);
        dto.setResourceType(ResourceType.OFFICIAL_PUBLICATION);
        doc.setApproveStatus(ApproveStatus.APPROVED);
        doc.setFilename("filename.txt");
        dto.setFile(
            new MockMultipartFile("name", "name.bin", "application/octet-stream", (byte[]) null));

        when(fileService.store(any(), eq("UUID"))).thenReturn("UUID.pdf");
        when(documentFileRepository.save(any())).thenReturn(doc);

        // when
        var actual = documentFileService.saveNewDocument(dto, true);

        // then
        assertEquals(doc.getFilename(), actual.getFilename());
    }

    @Test
    public void shouldThrowExceptionWhenCCLicenseIsNotProvided() {
        // given
        var dto = new DocumentFileDTO();
        var doc = new DocumentFile();
        dto.setAccessRights(AccessRights.OPEN_ACCESS);
        dto.setResourceType(ResourceType.OFFICIAL_PUBLICATION);
        doc.setApproveStatus(ApproveStatus.APPROVED);
        doc.setFilename("filename.txt");
        dto.setFile(
            new MockMultipartFile("name", "name.bin", "application/octet-stream", (byte[]) null));

        when(fileService.store(any(), eq("UUID"))).thenReturn("UUID.pdf");
        when(documentFileRepository.save(any())).thenReturn(doc);

        // when & then
        assertThrows(MissingDataException.class,
            () -> documentFileService.saveNewDocument(dto, true));
    }

    @Test
    public void shouldEditDocumentWhenValidDataIsProvided() {
        // given
        var doc = new DocumentFile();
        doc.setFilename("filename.txt");
        var dto = new DocumentFileDTO();
        dto.setId(1);
        dto.setAccessRights(AccessRights.ALL_RIGHTS_RESERVED);
        dto.setFile(
            new MockMultipartFile("name", "name.bin", "application/octet-stream", (byte[]) null));
        var docIndex = new DocumentFileIndex();

        when(documentFileRepository.findById(dto.getId())).thenReturn(Optional.of(doc));
        when(fileService.store(any(), eq("UUID"))).thenReturn("UUID.pdf");
        when(documentFileIndexRepository.findDocumentFileIndexByDatabaseId(any())).thenReturn(
            Optional.of(docIndex));
        when(documentFileRepository.save(any())).thenReturn(new DocumentFile());

        // when
        documentFileService.editDocumentFile(dto, false);

        // then
        verify(documentFileRepository, times(1)).save(any());
    }

    @Test
    public void shouldDeleteDocumentWhenValidDataIsProvided() {
        // given
        var doc = new DocumentFile();
        doc.setFilename("filename.txt");
        var dto = new DocumentFileDTO();
        dto.setFile(
            new MockMultipartFile("name", "name.bin", "application/octet-stream", (byte[]) null));

        when(documentFileRepository.findById(any())).thenReturn(Optional.of(doc));
        when(documentFileRepository.getReferenceByServerFilename("UUID")).thenReturn(doc);
        when(fileService.store(any(), eq("UUID"))).thenReturn("UUID.pdf");

        // when
        documentFileService.deleteDocumentFile("UUID");

        // then
        verify(documentFileRepository, times(1)).save(any());
        verify(documentFileRepository, never()).delete(any());
        verify(fileService, times(1)).delete(any());
    }

    @Test
    void shouldThrowLoadingExceptionWhenPdfFileCorrupted() {
        // given
        var documentFile = new DocumentFile();
        var multipartPdfFile =
            new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[] {});
        var serverFilename = "test.pdf";
        var documentIndex = new DocumentFileIndex();

        // when
        assertThrows(LoadingException.class,
            () -> documentFileService.parseAndIndexPdfDocument(documentFile, multipartPdfFile,
                serverFilename, documentIndex));

        // then (LoadingException should be thrown)
    }

    @Test
    public void shouldFindDocumentFileWhenSearchingWithSimpleQuery() {
        // given
        var tokens = Arrays.asList("neka", "ријеч", "za pretragu");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new DocumentFileIndex(), new DocumentFileIndex())));

        // when
        var result =
            documentFileService.searchDocumentFiles(new ArrayList<>(tokens), pageable,
                SearchRequestType.SIMPLE);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldFindDocumentFilesWhenSearchingWithAdvancedQuery() {
        // given
        var tokens = List.of("tile_sr:наслов");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new DocumentFileIndex(), new DocumentFileIndex())));

        // when
        var result =
            documentFileService.searchDocumentFiles(new ArrayList<>(tokens), pageable,
                SearchRequestType.ADVANCED);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void testChangeApproveStatus() throws IOException {
        // Given
        var documentFileId = 123;
        var approved = true;
        var documentFile = new DocumentFile();

        when(documentFileRepository.findById(documentFileId)).thenReturn(Optional.of(documentFile));
        when(fileService.loadAsResource(any())).thenReturn(
            new GetObjectResponse(null, null, null, null,
                new ByteArrayInputStream("Some test data".getBytes())));

        // When
        documentFileService.changeApproveStatus(documentFileId, approved);

        // Then
        verify(documentFileRepository, times(1)).findById(documentFileId);
        verify(fileService, times(1)).loadAsResource(any());
    }

    @ParameterizedTest
    @EnumSource(AccessRights.class)
    public void shouldReturnDocumentFileAccessLevelForAllLicenseTypes(AccessRights accessRights) {
        // given
        var documentFile = new DocumentFile();
        documentFile.setAccessRights(accessRights);

        when(documentFileRepository.getReferenceByServerFilename(any())).thenReturn(documentFile);

        // when
        var actual = documentFileService.getDocumentByServerFilename("serverFilename");

        // then
        assertEquals(accessRights, actual.getAccessRights());
    }
}
