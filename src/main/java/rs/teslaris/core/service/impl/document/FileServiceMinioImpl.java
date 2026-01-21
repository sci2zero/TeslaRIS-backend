package rs.teslaris.core.service.impl.document;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.tika.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
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
@Slf4j
public class FileServiceMinioImpl implements FileService {

    private final MinioClient minioClient;

    @Value("${spring.minio.bucket}")
    private String bucketName;


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

        try {
            PutObjectArgs args = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(serverFilename + "." + extension)
                .headers(Collections.singletonMap("Content-Disposition",
                    "attachment; filename=\"" + file.getOriginalFilename() + "\""))
                .stream(file.getInputStream(), file.getSize(), 0)
                .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new StorageException(
                "Error while storing file in Minio. Reason: " + e.getMessage());
        }

        return serverFilename + "." + extension;
    }

    @Override
    public String store(Resource resource, String serverFilename,
                        String originalFilename) {
        if (Objects.isNull(resource) || !resource.exists()) {
            throw new StorageException("Failed to store empty file.");
        }

        var originalFilenameTokens = originalFilename.split("\\.");
        var extension = originalFilenameTokens[originalFilenameTokens.length - 1];

        try {
            long size;
            try {
                if (resource instanceof FileSystemResource fileRes) {
                    size = fileRes.getFile().length();
                } else {
                    size = resource.contentLength();
                }
            } catch (IOException e) {
                size = -1;
            }

            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(serverFilename + "." + extension)
                .headers(Map.of(
                    "Content-Disposition",
                    "attachment; filename=\"" + originalFilename + "\""
                ));

            if (size >= 0) {
                builder.stream(resource.getInputStream(), size, -1);
            } else {
                builder.stream(resource.getInputStream(), -1, 5 * 1024 * 1024);
            }

            minioClient.putObject(builder.build());
        } catch (Exception e) {
            throw new StorageException(
                "Error while storing file in Minio. Reason: " + e.getMessage());
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
            throw new StorageException(
                "Error while deleting " + serverFilename + " from Minio. Reason: " +
                    e.getMessage());
        }
    }

    @Override
    public GetObjectResponse loadAsResource(String serverFilename) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(serverFilename)
                    .build()
            );

            var args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(serverFilename)
                .build();

            return Objects.requireNonNull(minioClient.getObject(args));
        } catch (Exception e) {
            throw new NotFoundException("Document " + serverFilename + " does not exist.");
        }
    }

    @Override
    public Pair<String, InputStream> duplicateFile(String serverFilename) {
        try {
            GetObjectArgs getArgs = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(serverFilename)
                .build();

            var file = minioClient.getObject(getArgs);

            var baos = new ByteArrayOutputStream();
            file.transferTo(baos);
            byte[] data = baos.toByteArray();

            InputStream uploadStream = new ByteArrayInputStream(data);

            var serverFilenameTokens = serverFilename.split("\\.");
            var extension = serverFilenameTokens[serverFilenameTokens.length - 1];
            var newServerFilename = UUID.randomUUID() + "." + extension;

            PutObjectArgs putArgs = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(newServerFilename)
                .stream(uploadStream, data.length, -1)
                .build();

            minioClient.putObject(putArgs);

            return new Pair<>(newServerFilename, new ByteArrayInputStream(data));

        } catch (Exception e) {
            throw new NotFoundException("Document " + serverFilename + " does not exist.");
        }
    }
}
