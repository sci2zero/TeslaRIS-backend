package rs.teslaris.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class BackupZipBuilder {

    @Getter
    private final Path rootDir;

    private final Path zipFile;

    public BackupZipBuilder(String prefix) throws IOException {
        this.rootDir = Files.createTempDirectory(prefix + "-data");
        this.zipFile = Files.createTempFile(prefix + "-", ".zip");
    }

    public Path createSubDir(String relativePath) throws IOException {
        Path subDir = rootDir.resolve(relativePath);
        Files.createDirectories(subDir);
        return subDir;
    }

    public void copyFile(InputStream in, Path target) throws IOException {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public void copyFileToRoot(InputStream inputStream, String fileName) throws IOException {
        Path targetPath = rootDir.resolve(fileName);
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public InputStreamResource buildZipAndGetResource() {
        try (var zipOut = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walk(rootDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String zipEntryName =
                            rootDir.relativize(path).toString().replace("\\", "/");
                        zipOut.putNextEntry(new ZipEntry(zipEntryName));
                        Files.copy(path, zipOut);
                        zipOut.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to zip file: " + path, e);
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException("Error while zipping", e);
        }

        try {
            return new InputStreamResource(Files.newInputStream(zipFile));
        } catch (IOException e) {
            throw new RuntimeException("Error returning zip resource", e);
        }
    }

    public void cleanup() {
        try {
            FileUtils.deleteDirectory(rootDir.toFile());
            Files.deleteIfExists(zipFile);
        } catch (IOException e) {
            log.error("Cleanup after backub generation has failed. Reason: {}",
                e.getMessage()); // should never happen
        }
    }

    public MultipartFile convertToMultipartFile(byte[] document, String fileName)
        throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(document);
        return new ResourceMultipartFile(fileName, fileName,
            "application/zip",
            new ByteArrayResource(byteArrayOutputStream.toByteArray()));
    }
}

