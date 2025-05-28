package rs.teslaris.core.service.impl.document;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import okhttp3.Headers;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.tika.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;

@Service
@RequiredArgsConstructor
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
    public void delete(String serverFilename) {
        var rootPath = Paths.get(rootLocation).toAbsolutePath().normalize();
        var targetPath = rootPath.resolve(serverFilename).normalize();

        if (!targetPath.startsWith(rootPath)) {
            throw new StorageException("Invalid path: " + serverFilename);
        }

        var file = targetPath.toFile();
        if (!file.delete()) {
            throw new StorageException("Failed to delete " + serverFilename + ".");
        }
    }

    @Override
    public GetObjectResponse loadAsResource(String filename) throws IOException {
        var filepath = Paths.get(rootLocation, filename).normalize().toAbsolutePath();

        // Ensure file stays within rootLocation
        if (!filepath.startsWith(Paths.get(rootLocation).toAbsolutePath())) {
            throw new StorageException("Access denied: " + filename);
        }

        Resource resource;
        try {
            resource = new UrlResource(filepath.toUri());
        } catch (MalformedURLException e) {
            throw new StorageException("Could not read file: " + filename);
        }

        if (resource.exists() && resource.isReadable()) {
            try (var inputStream = resource.getInputStream()) {
                var headersMap = new HashMap<String, String>();
                headersMap.put("Content-Type", "application/octet-stream");
                var headers = Headers.of(headersMap);

                return new GetObjectResponse(headers, null, null, null, inputStream);
            }
        } else {
            throw new StorageException("Could not read file: " + filename);
        }
    }
}
