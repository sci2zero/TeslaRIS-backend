package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.service.impl.document.FileServiceMinioImpl;

@SpringBootTest
public class FileServiceMinioTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private FileServiceMinioImpl fileService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(fileService, "bucketName", "bucket-name");
    }

    private MultipartFile createMockMultipartFile(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/plain", content.getBytes());
    }

    @Test
    public void ShouldStoreNonEmptyFile() throws Exception {
        // given
        var file = createMockMultipartFile("test.txt", "Test file content");
        var serverFilename = "file1";

        // when
        var result = fileService.store(file, serverFilename);

        // then
        assertEquals("file1.txt", result);
        verify(minioClient, times(1)).putObject(any());
    }

    @Test
    public void ShouldThrowExceptionWhenStoringEmptyFile() {
        // given
        var file = createMockMultipartFile("empty.txt", "");
        var serverFilename = "file2";

        // when
        assertThrows(StorageException.class, () -> fileService.store(file, serverFilename));

        // then (StorageException should be thrown)
    }

    @Test
    public void ShouldLoadExistingReadableResource() throws Exception {
        // given
        var filename = "file1.txt";

        when(minioClient.getObject(any())).thenReturn(
            new GetObjectResponse(null, null, null, null, null));

        // when
        var resource = fileService.loadAsResource(filename);

        // then
        assertNotNull(resource);
        verify(minioClient, times(1)).getObject(any());
    }

    @Test
    public void ShouldThrowExceptionWhenLoadingNonExistingResource() {
        // given
        var filename = "nonExistingFile.txt";

        // when
        assertThrows(NotFoundException.class, () -> fileService.loadAsResource(filename));

        // then (StorageException should be thrown)
    }

    @Test
    public void ShouldDeleteFileWhenItExists() throws Exception {
        // given
        var serverFilename = "file1.txt";

        // when
        fileService.delete(serverFilename);

        // then
        verify(minioClient, times(1)).removeObject(any());
    }
}
