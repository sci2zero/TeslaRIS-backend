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

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import rs.teslaris.core.service.impl.document.FileServiceS3Impl;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@SpringBootTest
public class FileServiceS3Test {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private FileServiceS3Impl fileService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(fileService, "bucketName", "bucket-name");
    }

    private MultipartFile createMockMultipartFile(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/plain", content.getBytes());
    }

    private ResponseInputStream<GetObjectResponse> objectStream(String content) {
        return new ResponseInputStream<>(GetObjectResponse.builder().build(),
            AbortableInputStream.create(new ByteArrayInputStream(content.getBytes())));
    }

    @Test
    public void shouldStoreNonEmptyFile() {
        // given
        var file = createMockMultipartFile("test.txt", "Test file content");
        var serverFilename = "file1";

        // when
        var result = fileService.store(file, serverFilename);

        // then
        assertEquals("file1.txt", result);

        var requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertEquals("bucket-name", requestCaptor.getValue().bucket());
        assertEquals("file1.txt", requestCaptor.getValue().key());
        assertEquals("attachment; filename=\"test.txt\"",
            requestCaptor.getValue().contentDisposition());
    }

    @Test
    public void shouldThrowExceptionWhenStoringEmptyFile() {
        // given
        var file = createMockMultipartFile("empty.txt", "");
        var serverFilename = "file2";

        // when
        assertThrows(StorageException.class, () -> fileService.store(file, serverFilename));

        // then
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    public void shouldLoadExistingReadableResource() {
        // given
        var filename = "file1.txt";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
            .thenReturn(HeadObjectResponse.builder().build());
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(objectStream("data"));

        // when
        var resource = fileService.loadAsResource(filename);

        // then
        assertNotNull(resource);
        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    public void shouldThrowExceptionWhenLoadingNonExistingResource() {
        // given
        var filename = "nonExistingFile.txt";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder().build());

        // when & then
        assertThrows(NotFoundException.class, () -> fileService.loadAsResource(filename));
        verify(s3Client, never()).getObject(any(GetObjectRequest.class));
    }

    @Test
    public void shouldDeleteFileWhenItExists() {
        // given
        var serverFilename = "file1.txt";

        // when
        fileService.delete(serverFilename);

        // then
        var requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(requestCaptor.capture());
        assertEquals("bucket-name", requestCaptor.getValue().bucket());
        assertEquals("file1.txt", requestCaptor.getValue().key());
    }

    @Test
    public void shouldStoreValidResource() {
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

        // when
        String result = fileService.store(resource, serverFilename, originalFilename);

        // then
        assertEquals("file1.txt", result);

        var requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertEquals("file1.txt", requestCaptor.getValue().key());
        assertEquals("attachment; filename=\"test.txt\"",
            requestCaptor.getValue().contentDisposition());
    }

    @Test
    public void shouldThrowExceptionWhenResourceIsNull() {
        // given
        Resource resource = null;
        String serverFilename = "file2";
        String originalFilename = "test.txt";

        // when & then
        assertThrows(StorageException.class, () ->
            fileService.store(resource, serverFilename, originalFilename));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    public void shouldThrowExceptionWhenResourceDoesNotExist() {
        // given
        Resource resource = mock(Resource.class);
        when(resource.exists()).thenReturn(false);

        String serverFilename = "file3";
        String originalFilename = "test.txt";

        // when & then
        assertThrows(StorageException.class, () ->
            fileService.store(resource, serverFilename, originalFilename));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    public void shouldHandleResourceWithUnknownSize() throws IOException {
        // given
        String content = "Test content";
        var resource = mock(Resource.class);
        when(resource.exists()).thenReturn(true);
        when(resource.contentLength()).thenThrow(new IOException("Cannot determine size"));
        when(resource.getInputStream())
            .thenReturn(new ByteArrayInputStream(content.getBytes()));

        String serverFilename = "file4";
        String originalFilename = "test.txt";

        // when
        String result = fileService.store(resource, serverFilename, originalFilename);

        // then
        assertEquals("file4.txt", result);

        var requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertEquals("file4.txt", requestCaptor.getValue().key());
    }

    @Test
    public void shouldDuplicateExistingFile() {
        // given
        var serverFilename = "original.txt";
        when(s3Client.getObject(any(GetObjectRequest.class)))
            .thenReturn(objectStream("duplicate me"));

        // when
        var result = fileService.duplicateFile(serverFilename);

        // then
        assertNotNull(result.a);
        assertEquals("txt", result.a.substring(result.a.lastIndexOf('.') + 1));

        var requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertEquals(result.a, requestCaptor.getValue().key());
    }

    @Test
    public void shouldThrowExceptionWhenDuplicatingNonExistingFile() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder().build());

        // when & then
        assertThrows(NotFoundException.class, () -> fileService.duplicateFile("missing.txt"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
