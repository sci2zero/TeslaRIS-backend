package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.service.impl.document.FileServiceFileSystemImpl;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileServiceTest {

    private final String testRootPath = "src/main/resources/dataTest";

    @InjectMocks
    private FileServiceFileSystemImpl fileService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(fileService, "rootLocation", testRootPath);
    }

    private MultipartFile createMockMultipartFile(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/plain", content.getBytes());
    }

    @Test
    @Order(1)
    public void ShouldStoreNonEmptyFile() {
        // given
        var file = createMockMultipartFile("test.txt", "Test file content");
        var serverFilename = "file1";

        // when
        var result = fileService.store(file, serverFilename);

        // then
        assertEquals("file1.txt", result);
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
    public void ShouldThrowExceptionWhenStoringFileOutsideCurrentDirectory() {
        // given
        var file = createMockMultipartFile("test.txt", "Test file content");
        var serverFilename = "../file3";

        // when
        assertThrows(StorageException.class, () -> fileService.store(file, serverFilename));

        // then (StorageException should be thrown)
    }

    @Test
    @Order(2)
    public void ShouldLoadExistingReadableResource() throws IOException {
        // given
        var filename = "file1.txt";

        // when
        var resource = fileService.loadAsResource(filename);

        // then
        assertNotNull(resource);
    }

    @Test
    public void ShouldThrowExceptionWhenLoadingNonExistingResource() {
        // given
        var filename = "nonExistingFile.txt";

        // when
        assertThrows(StorageException.class, () -> fileService.loadAsResource(filename));

        // then (StorageException should be thrown)
    }

    @Test
    @Order(3)
    public void ShouldDeleteFile() throws IOException {
        // given
        var serverFilename = "file1.txt";
        var file = new File(Paths.get(testRootPath, serverFilename).toUri());

        // when
        fileService.delete(serverFilename);

        // then
        assertFalse(file.exists());
    }

    @Test
    public void ShouldThrowExceptionWhenDeleteFileDoesNotExist() throws IOException {
        // given
        var serverFilename = "nonExistingFile.txt";

        // when
        assertThrows(StorageException.class, () -> fileService.delete(serverFilename));

        // then (StorageException should be thrown)
    }

}
