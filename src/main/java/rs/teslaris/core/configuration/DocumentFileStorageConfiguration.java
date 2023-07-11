package rs.teslaris.core.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.impl.document.FileServiceImpl;
import rs.teslaris.core.service.impl.document.FileServiceMinioImpl;

@Configuration
@RequiredArgsConstructor
public class DocumentFileStorageConfiguration {

    private final FileServiceImpl filesystemImpl;

    private final FileServiceMinioImpl minioImpl;

    @Value("${document.file.storage}")
    private String implementation;


    @Bean
    public FileService fileService() {
        switch (implementation) {
            case "FileSystemStorage":
                return filesystemImpl;
            case "MINIOStorage":
                return minioImpl;
            default:
                throw new IllegalArgumentException(
                    "Invalid document.file.storage property value: " + implementation);
        }
    }
}
