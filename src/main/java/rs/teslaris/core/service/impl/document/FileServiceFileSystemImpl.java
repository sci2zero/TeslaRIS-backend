package rs.teslaris.core.service.impl.document;

import io.minio.GetObjectResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import okhttp3.Headers;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.tika.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.functional.Pair;

@Service
@RequiredArgsConstructor
@Traceable
public class FileServiceFileSystemImpl implements FileService {

    @Value("${document_storage.root_path}")
    private String rootLocation;


    @Override
    public String store(MultipartFile file, String serverFilename) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }

        var originalFilename = FilenameUtils.normalize(
            Objects.requireNonNullElse(file.getOriginalFilename(), ""));
        if (originalFilename.isEmpty()) {
            throw new StorageException("Failed to store file with empty file name.");
        }

        var originalFilenameTokens =
            Objects.requireNonNull(StringEscapeUtils.escapeHtml4(originalFilename))
                .split("\\.");
        var extension = originalFilenameTokens[originalFilenameTokens.length - 1];
        var destinationFilePath = Paths.get(rootLocation, serverFilename + "." + extension)
            .normalize().toAbsolutePath();

        if (!destinationFilePath.getParent().endsWith(rootLocation)) {
            throw new StorageException(
                "Cannot store file outside current directory.");
        }

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFilePath,
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store file.");
        }

        return serverFilename + "." + extension;
    }

    @Override
    public String store(Resource resource, String serverFilename,
                        String originalFilename) {
        if (Objects.isNull(resource) || !resource.exists()) {
            throw new StorageException("Failed to store empty file.");
        }

        var sanitizedOriginalFilename = FilenameUtils.normalize(
            Objects.requireNonNullElse(originalFilename, ""));
        if (sanitizedOriginalFilename.isEmpty()) {
            throw new StorageException("Failed to store file with empty file name.");
        }

        var originalFilenameTokens =
            Objects.requireNonNull(StringEscapeUtils.escapeHtml4(sanitizedOriginalFilename))
                .split("\\.");
        var extension = originalFilenameTokens[originalFilenameTokens.length - 1];
        var destinationFilePath = Paths.get(rootLocation, serverFilename + "." + extension)
            .normalize().toAbsolutePath();

        if (!destinationFilePath.getParent().endsWith(rootLocation)) {
            throw new StorageException(
                "Cannot store file outside current directory.");
        }

        try (var inputStream = resource.getInputStream()) {
            Files.copy(inputStream, destinationFilePath,
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store file.");
        }

        return serverFilename + "." + extension;
    }

    @Override
    public void delete(String serverFilename) {
        var rootPath = Paths.get(rootLocation).toAbsolutePath().normalize();
        var targetPath = rootPath.resolve(serverFilename).normalize();

        if (!targetPath.startsWith(rootPath)) {
            throw new StorageException("Invalid path: " + serverFilename);
        }

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to delete " + serverFilename + ": " + e.getMessage());
        }
    }

    @Override
    public GetObjectResponse loadAsResource(String filename) {
        var rootPath = Paths.get(rootLocation).toAbsolutePath().normalize();
        var filepath = rootPath.resolve(filename).normalize();

        if (!filepath.startsWith(rootPath)) {
            throw new StorageException("Access denied: " + filename);
        }

        if (!Files.exists(filepath) || !Files.isReadable(filepath)) {
            throw new NotFoundException("Document " + filename + " does not exist.");
        }

        try {
            var filePath = filepath.toAbsolutePath();
            long fileSize = Files.size(filePath);
            var contentType = Files.probeContentType(filePath);
            var etag = calculateFileETag(filePath);

            var headersMap = new HashMap<String, String>();
            headersMap.put("Content-Type",
                Objects.requireNonNullElse(contentType, "application/octet-stream"));
            headersMap.put("Content-Length", String.valueOf(fileSize));
            headersMap.put("ETag", etag);
            headersMap.put("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            var headers = Headers.of(headersMap);
            var inputStream = Files.newInputStream(filePath);

            return new GetObjectResponse(headers, null, null, null, inputStream) {
                @Override
                public void close() {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // Log warning but don't throw
                        System.err.println(
                            "Warning: Failed to close input stream: " + e.getMessage());
                    }
                }
            };

        } catch (IOException e) {
            throw new StorageException("Could not read file: " + filename + " - " + e.getMessage());
        }
    }

    @Override
    public Pair<String, InputStream> duplicateFile(String serverFilename) {
        var rootPath = Paths.get(rootLocation).toAbsolutePath().normalize();
        var sourcePath = rootPath.resolve(serverFilename).normalize();

        if (!sourcePath.startsWith(rootPath)) {
            throw new StorageException("Access denied: " + serverFilename);
        }

        if (!Files.exists(sourcePath) || !Files.isReadable(sourcePath)) {
            throw new NotFoundException("Document " + serverFilename + " does not exist.");
        }

        try {
            byte[] fileData = Files.readAllBytes(sourcePath);

            var serverFilenameTokens = serverFilename.split("\\.");
            var extension = serverFilenameTokens[serverFilenameTokens.length - 1];
            var newServerFilename = UUID.randomUUID() + "." + extension;
            var destinationPath = rootPath.resolve(newServerFilename).normalize();

            if (!destinationPath.startsWith(rootPath)) {
                throw new StorageException("Cannot store file outside current directory.");
            }

            Files.write(destinationPath, fileData);

            var uploadStream = new ByteArrayInputStream(fileData);

            return new Pair<>(newServerFilename, uploadStream);

        } catch (IOException e) {
            throw new StorageException(
                "Failed to duplicate file " + serverFilename + ": " + e.getMessage());
        }
    }

    private String calculateFileETag(Path filePath) throws IOException {
        var fileAttributes = Files.readAttributes(filePath, BasicFileAttributes.class);
        var eTagData =
            filePath.toString() + fileAttributes.lastModifiedTime() + fileAttributes.size();
        return "\"" + Integer.toHexString(eTagData.hashCode()) + "\"";
    }
}
