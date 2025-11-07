package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.service.impl.document.FileServiceFileSystemImpl;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
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
        assertThrows(NotFoundException.class, () -> fileService.loadAsResource(filename));

        // then (NotFoundException should be thrown)
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

        try (MockedStatic<Files> converter = mockStatic(Files.class)) {
            converter.when(() -> Files.deleteIfExists(any())).thenThrow(IOException.class);

            // when
            assertThrows(StorageException.class, () -> fileService.delete(serverFilename));

            // then (StorageException should be thrown)
        }
    }

    @Test
    public void ShouldDuplicateExistingFile() {
        // given
        var serverFilename = "existing-file.txt";
        var originalContent = "Original file content";
        var rootPath = Paths.get(testRootPath).toAbsolutePath().normalize();
        var sourcePath = rootPath.resolve(serverFilename);

        try {
            Files.write(sourcePath, originalContent.getBytes());
        } catch (IOException e) {
            fail("Failed to setup test file");
        }

        // when
        var result = fileService.duplicateFile(serverFilename);

        // then
        assertNotNull(result);
        assertNotNull(result.a);
        assertNotNull(result.b);

        assertTrue(result.a.endsWith(".txt"));

        try {
            var duplicatedContent = new String(result.b.readAllBytes());
            assertEquals(originalContent, duplicatedContent);
        } catch (IOException e) {
            fail("Failed to read duplicated content");
        }

        var newFilePath = rootPath.resolve(result.a);
        assertTrue(Files.exists(newFilePath));

        // Cleanup
        try {
            Files.deleteIfExists(sourcePath);
            Files.deleteIfExists(newFilePath);
        } catch (IOException e) {
            // Ignore cleanup errors, if any
        }
    }

    @Test
    public void ShouldThrowExceptionWhenDuplicatingNonExistentFile() {
        // given
        var nonExistentFilename = "non-existent-file.txt";

        // when & then
        assertThrows(NotFoundException.class, () -> {
            fileService.duplicateFile(nonExistentFilename);
        });
    }

    @Test
    public void ShouldThrowExceptionWhenDuplicatingFileOutsideRoot() {
        // given
        var maliciousFilename = "../outside-file.txt";

        // when & then
        assertThrows(StorageException.class, () -> {
            fileService.duplicateFile(maliciousFilename);
        });
    }

    @Test
    public void ShouldDuplicateFileWithComplexExtension() {
        // given
        var serverFilename = "document.tar.gz";
        var originalContent = "Compressed file content";
        var rootPath = Paths.get(testRootPath).toAbsolutePath().normalize();
        var sourcePath = rootPath.resolve(serverFilename);

        try {
            Files.write(sourcePath, originalContent.getBytes());
        } catch (IOException e) {
            fail("Failed to setup test file");
        }

        // when
        var result = fileService.duplicateFile(serverFilename);

        // then
        assertNotNull(result);

        assertTrue(result.a.endsWith(".gz"));

        try {
            var duplicatedContent = new String(result.b.readAllBytes());
            assertEquals(originalContent, duplicatedContent);
        } catch (IOException e) {
            fail("Failed to read duplicated content");
        }

        // Cleanup
        try {
            Files.deleteIfExists(sourcePath);
            Files.deleteIfExists(rootPath.resolve(result.a));
        } catch (IOException e) {
            // Ignore cleanup errors, if any
        }
    }

    @Test
    public void ShouldGenerateUniqueFilenamesWhenDuplicating() {
        // given
        var serverFilename = "original.doc";
        var originalContent = "Document content";
        var rootPath = Paths.get(testRootPath).toAbsolutePath().normalize();
        var sourcePath = rootPath.resolve(serverFilename);

        try {
            Files.write(sourcePath, originalContent.getBytes());
        } catch (IOException e) {
            fail("Failed to setup test file");
        }

        // when
        var result1 = fileService.duplicateFile(serverFilename);
        var result2 = fileService.duplicateFile(serverFilename);

        // then
        assertNotNull(result1.a);
        assertNotNull(result2.a);
        assertNotEquals(result1.a, result2.a);

        assertTrue(result1.a.endsWith(".doc"));
        assertTrue(result2.a.endsWith(".doc"));

        // Cleanup
        try {
            Files.deleteIfExists(sourcePath);
            Files.deleteIfExists(rootPath.resolve(result1.a));
            Files.deleteIfExists(rootPath.resolve(result2.a));
        } catch (IOException e) {
            // Ignore cleanup errors, if any
        }
    }

    @Test
    public void ShouldHandleLargeFilesWhenDuplicating() {
        // given
        var serverFilename = "large-file.bin";
        var largeContent = new byte[1024 * 1024];
        new Random().nextBytes(largeContent);
        var rootPath = Paths.get(testRootPath).toAbsolutePath().normalize();
        var sourcePath = rootPath.resolve(serverFilename);

        try {
            Files.write(sourcePath, largeContent);
        } catch (IOException e) {
            fail("Failed to setup test file");
        }

        // when
        var result = fileService.duplicateFile(serverFilename);

        // then
        assertNotNull(result);

        try {
            var duplicatedContent = result.b.readAllBytes();
            assertArrayEquals(largeContent, duplicatedContent);
        } catch (IOException e) {
            fail("Failed to read duplicated large file content");
        }

        // Cleanup
        try {
            Files.deleteIfExists(sourcePath);
            Files.deleteIfExists(rootPath.resolve(result.a));
        } catch (IOException e) {
            // Ignore cleanup errors, if any
        }
    }
}
