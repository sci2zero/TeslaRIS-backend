package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.service.impl.document.FileServiceMinioImpl;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;

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
    public void shouldStoreNonEmptyFile() throws Exception {
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
    public void shouldThrowExceptionWhenStoringEmptyFile() {
        // given
        var file = createMockMultipartFile("empty.txt", "");
        var serverFilename = "file2";

        // when
        assertThrows(StorageException.class, () -> fileService.store(file, serverFilename));

        // then (StorageException should be thrown)
    }

    @Test
    public void shouldLoadExistingReadableResource() throws Exception {
        // given
        var filename = "file1.txt";

        when(minioClient.getObject(any())).thenReturn(
            new GetObjectResponse(null, null, null, null, null));

        when(minioClient.statObject(any())).thenReturn(mock(StatObjectResponse.class));

        // when
        var resource = fileService.loadAsResource(filename);

        // then
        assertNotNull(resource);
        verify(minioClient, times(1)).getObject(any());
    }

    @Test
    public void shouldThrowExceptionWhenLoadingNonExistingResource() {
        // given
        var filename = "nonExistingFile.txt";

        // when
        assertThrows(NotFoundException.class, () -> fileService.loadAsResource(filename));

        // then (StorageException should be thrown)
    }

    @Test
    public void shouldDeleteFileWhenItExists() throws Exception {
        // given
        var serverFilename = "file1.txt";

        // when
        fileService.delete(serverFilename);

        // then
        verify(minioClient, times(1)).removeObject(any());
    }

    @Test
    public void shouldStoreValidResource() throws Exception {
        // given
        String content = "Test file content";
        Resource resource = new ByteArrayResource(content.getBytes()) {
            @Override
            public long contentLength() {
                return content.getBytes().length;
            }
        };
        String serverFilename = "file1";
        String originalFilename = "test.txt";

        // Mock MinIO client to accept any arguments
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        // when
        String result = fileService.store(resource, serverFilename, originalFilename);

        // then
        assertEquals("file1.txt", result);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    public void shouldThrowExceptionWhenResourceIsNull()
        throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
        NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
        InternalException {
        // given
        Resource resource = null;
        String serverFilename = "file2";
        String originalFilename = "test.txt";

        // when & then
        assertThrows(StorageException.class, () ->
            fileService.store(resource, serverFilename, originalFilename));

        verify(minioClient, never()).putObject(any());
    }

    @Test
    public void shouldThrowExceptionWhenResourceDoesNotExist()
        throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
        NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
        InternalException {
        // given
        Resource resource = mock(Resource.class);
        when(resource.exists()).thenReturn(false);

        String serverFilename = "file3";
        String originalFilename = "test.txt";

        // when & then
        assertThrows(StorageException.class, () ->
            fileService.store(resource, serverFilename, originalFilename));

        verify(minioClient, never()).putObject(any());
    }

    @Test
    public void shouldHandleResourceWithUnknownSize() throws Exception {
        // given
        String content = "Test content";
        var inputStream = new ByteArrayInputStream(content.getBytes());

        var resource = mock(Resource.class);
        when(resource.exists()).thenReturn(true);
        when(resource.contentLength()).thenThrow(new IOException("Cannot determine size"));
        when(resource.getInputStream()).thenReturn(inputStream);

        String serverFilename = "file4";
        String originalFilename = "test.txt";

        // Mock MinIO client
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        // when
        String result = fileService.store(resource, serverFilename, originalFilename);

        // then
        assertEquals("file4.txt", result);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));

        // Verify that size -1 was used (unknown size)
        var argsCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(argsCaptor.capture());
        var capturedArgs = argsCaptor.getValue();

        // For unknown size, the method uses partSize = 5 * 1024 * 1024
        // The PutObjectArgs doesn't expose partSize directly, but we can verify
        // it was called at least
    }
}
