package rs.teslaris.core.service.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.exception.StorageException;
import rs.teslaris.core.service.FileService;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    @Value("${document_storage.root_path}")
    private String rootLocation;

    @Override
    public String store(MultipartFile file, String serverFilename) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }

        var originalFilenameTokens =
            Objects.requireNonNull(file.getOriginalFilename()).split("\\.");
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
        var file = new File(Paths.get(rootLocation, serverFilename).toUri());
        if (!file.delete()) {
            throw new StorageException("Failed to delete " + serverFilename + " .");
        }
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
