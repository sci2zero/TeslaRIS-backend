package rs.teslaris.core.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.exception.StorageException;
import rs.teslaris.core.service.FileService;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final String rootLocation = "src/main/resources/data";

    @Override
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }

        var originalFilenameTokens =
            Objects.requireNonNull(file.getOriginalFilename()).split("\\.");
        var extension = originalFilenameTokens[originalFilenameTokens.length - 1];
        var serverFilename = UUID.randomUUID().toString();
        var destinationFilePath = Paths.get(rootLocation, serverFilename + "." + extension)
            .normalize().toAbsolutePath();

        if (!destinationFilePath.getParent().endsWith("src/main/resources/data")) {
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
    public String detectMimeType(MultipartFile file) {
        var contentAnalyzer = new Tika();

        String trueMimeType;
        String specifiedMimeType;
        try {
            trueMimeType = contentAnalyzer.detect(file.getBytes());
            specifiedMimeType = Files.probeContentType(Path.of(
                Objects.requireNonNull(file.getOriginalFilename())));
        } catch (IOException e) {
            throw new StorageException("Failed to detect mime type for file.");
        }

        if (!trueMimeType.equals(specifiedMimeType) &&
            !(trueMimeType.contains("zip") && specifiedMimeType.contains("zip"))) {
            throw new StorageException("True mime type is different from specified one, aborting.");
        }

        return trueMimeType;
    }

    @Override
    public Resource loadAsResource(String filename) {
        var filepath = Paths.get(rootLocation, filename)
            .normalize().toAbsolutePath();

        Resource resource;
        try {
            resource = new UrlResource(filepath.toUri());
        } catch (MalformedURLException e) {
            throw new StorageException("Could not read file: " + filename);
        }

        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new StorageException("Could not read file: " + filename);
        }
    }
}
