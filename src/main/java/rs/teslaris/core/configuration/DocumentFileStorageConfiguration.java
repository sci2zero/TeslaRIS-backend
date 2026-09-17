package rs.teslaris.core.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rs.teslaris.core.service.impl.document.FileServiceFileSystemImpl;
import rs.teslaris.core.service.impl.document.FileServiceS3Impl;
import rs.teslaris.core.service.interfaces.document.FileService;

@Configuration
@RequiredArgsConstructor
public class DocumentFileStorageConfiguration {

    private final FileServiceFileSystemImpl filesystemImpl;

    private final FileServiceS3Impl s3Impl;

    @Value("${document.file.storage}")
    private String implementation;


    @Bean
    public FileService fileService() {
        return switch (implementation) {
            case "FileSystemStorage" -> filesystemImpl;
            case "S3Storage" -> s3Impl;
            default -> throw new IllegalArgumentException(
                "Invalid document.file.storage property value: " + implementation);
        };
    }
}
