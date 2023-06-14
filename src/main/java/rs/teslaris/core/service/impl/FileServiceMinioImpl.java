package rs.teslaris.core.service.impl;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.exception.StorageException;
import rs.teslaris.core.service.FileService;

@Service
@RequiredArgsConstructor
@Profile("MINIOStorage")
public class FileServiceMinioImpl implements FileService {

    private final MinioClient minioClient;

    @Value("${spring.minio.bucket}")
    private String bucketName;

    @Override
    public String store(MultipartFile file, String serverFilename) {
        var originalFilenameTokens =
            Objects.requireNonNull(file.getOriginalFilename()).split("\\.");
        var extension = originalFilenameTokens[originalFilenameTokens.length - 1];

        try {
            PutObjectArgs args = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(serverFilename + "." + extension)
                .stream(file.getInputStream(), file.getInputStream().available(), -1)
                .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new StorageException("Error while storing file in Minio.");
        }

        return serverFilename + "." + extension;
    }

    @Override
    public void delete(String serverFilename) {
        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(serverFilename)
                .build();
            minioClient.removeObject(args);
        } catch (Exception e) {
            throw new StorageException("Error while deleting " + serverFilename + " from Minio.");
        }
    }

    @Override
    public Resource loadAsResource(String serverFilename) {
        try {
            var args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(serverFilename)
                .build();
            return new InputStreamResource(minioClient.getObject(args));
        } catch (Exception e) {
            throw new NotFoundException("Document " + serverFilename + " does not exist.");
        }
    }
}
