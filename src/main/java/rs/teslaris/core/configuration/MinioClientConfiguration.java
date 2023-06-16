package rs.teslaris.core.configuration;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioClientConfiguration {

    @Value("${spring.minio.url}")
    private String minioHost;

    @Value("${spring.minio.access-key}")
    private String minioAccessKey;

    @Value("${spring.minio.secret-key}")
    private String minioSecretKey;
    
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder().endpoint(minioHost).credentials(minioAccessKey, minioSecretKey)
            .build();
    }
}
