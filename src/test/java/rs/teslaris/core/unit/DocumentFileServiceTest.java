package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.indexrepository.DocumentFileIndexRepository;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.repository.document.DocumentFileRepository;
import rs.teslaris.core.service.impl.document.DocumentFileServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

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

    @InjectMocks
    private DocumentFileServiceImpl documentFileService;

    @Test
    public void shouldReturnDocumentFileWhenValidIdIsProvided() {
        // given
        var documentFile = new DocumentFile();

        when(documentFileRepository.findById(1)).thenReturn(Optional.of(documentFile));

        // when
        var actual = documentFileService.findDocumentFileById(1);

        // then
        assertEquals(documentFile, actual);
    }

    @Test
    public void shouldThrowExceptionWhenInvalidIdIsProvided() {
        // given
        when(documentFileRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> documentFileService.findDocumentFileById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldSaveNewDocumentWhenValidDataIsProvided() throws IOException {
        // given
        var dto = new DocumentFileDTO();
        var doc = new DocumentFile();
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
    public void shouldEditDocumentWhenValidDataIsProvided() throws IOException {
        // given
        var doc = new DocumentFile();
        doc.setFilename("filename.txt");
        var dto = new DocumentFileDTO();
        dto.setId(1);
        dto.setFile(
            new MockMultipartFile("name", "name.bin", "application/octet-stream", (byte[]) null));
        var docIndex = new DocumentFileIndex();

        when(documentFileRepository.findById(dto.getId())).thenReturn(Optional.of(doc));
        when(fileService.store(any(), eq("UUID"))).thenReturn("UUID.pdf");
        when(documentFileIndexRepository.findDocumentFileIndexByDatabaseId(any())).thenReturn(
            Optional.of(docIndex));

        // when
        documentFileService.editDocumentFile(dto);

        // then
        verify(documentFileRepository, times(1)).save(any());
    }

    @Test
    public void shouldDeleteDocumentWhenValidDataIsProvided() throws IOException {
        // given
        var doc = new DocumentFile();
        doc.setFilename("filename.txt");
        var dto = new DocumentFileDTO();
        dto.setFile(
            new MockMultipartFile("name", "name.bin", "application/octet-stream", (byte[]) null));

        when(documentFileRepository.getReferenceByServerFilename("UUID")).thenReturn(doc);
        when(fileService.store(any(), eq("UUID"))).thenReturn("UUID.pdf");

        // when
        documentFileService.deleteDocumentFile("UUID");

        // then
        verify(documentFileRepository, times(1)).delete(any());
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
}
