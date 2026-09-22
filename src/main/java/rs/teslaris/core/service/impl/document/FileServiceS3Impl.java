package rs.teslaris.core.service.impl.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Traceable
@Slf4j
public class FileServiceS3Impl implements FileService {

    private final S3Client s3Client;

    @Value("${spring.s3.bucket}")
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
            var request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(serverFilename + "." + extension)
                .contentDisposition(
                    "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .build();
            s3Client.putObject(request,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            throw new StorageException(
                "Error while storing file in S3. Reason: " + e.getMessage());
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
            var request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(serverFilename + "." + extension)
                .contentDisposition("attachment; filename=\"" + originalFilename + "\"")
                .build();

            if (resource instanceof FileSystemResource fileRes) {
                s3Client.putObject(request, RequestBody.fromFile(fileRes.getFile()));
            } else {
                long size;
                try {
                    size = resource.contentLength();
                } catch (IOException e) {
                    size = -1;
                }

                if (size >= 0) {
                    s3Client.putObject(request,
                        RequestBody.fromInputStream(resource.getInputStream(), size));
                } else {
                    uploadUnknownLength(request, resource.getInputStream());
                }
            }
        } catch (Exception e) {
            throw new StorageException(
                "Error while storing file in S3. Reason: " + e.getMessage());
        }

        return serverFilename + "." + extension;
    }

    private void uploadUnknownLength(PutObjectRequest request, InputStream inputStream)
        throws IOException {
        var tempFile = Files.createTempFile("s3-upload-", ".tmp");
        try (inputStream) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            s3Client.putObject(request, RequestBody.fromFile(tempFile));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public void delete(String serverFilename) {
        try {
            var request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(serverFilename)
                .build();
            s3Client.deleteObject(request);
        } catch (Exception e) {
            throw new StorageException(
                "Error while deleting " + serverFilename + " from S3. Reason: " +
                    e.getMessage());
        }
    }

    @Override
    public ResponseInputStream<GetObjectResponse> loadAsResource(String serverFilename) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(serverFilename)
                .build());

            var request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(serverFilename)
                .build();

            return Objects.requireNonNull(s3Client.getObject(request));
        } catch (Exception e) {
            throw new NotFoundException("Document " + serverFilename + " does not exist.");
        }
    }

    @Override
    public Pair<String, InputStream> duplicateFile(String serverFilename) {
        try {
            var getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(serverFilename)
                .build();

            byte[] data;
            try (var file = s3Client.getObject(getRequest)) {
                var baos = new ByteArrayOutputStream();
                file.transferTo(baos);
                data = baos.toByteArray();
            }

            var serverFilenameTokens = serverFilename.split("\\.");
            var extension = serverFilenameTokens[serverFilenameTokens.length - 1];
            var newServerFilename = UUID.randomUUID() + "." + extension;

            var putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(newServerFilename)
                .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(data));

            return new Pair<>(newServerFilename, new ByteArrayInputStream(data));

        } catch (Exception e) {
            throw new NotFoundException("Document " + serverFilename + " does not exist.");
        }
    }
}
