package rs.teslaris.core.configuration;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3ClientConfiguration {

    @Value("${spring.s3.url}")
    private String s3Url;

    @Value("${spring.s3.access-key}")
    private String s3AccessKey;

    @Value("${spring.s3.secret-key}")
    private String s3SecretKey;

    @Value("${spring.s3.region:us-east-1}")
    private String s3Region;


    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .endpointOverride(URI.create(s3Url))
            .region(Region.of(s3Region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3AccessKey, s3SecretKey)))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
            // Most S3-compatible stores do not support the flexible checksum
            // trailers that the SDK sends by default since 2.30, uploads fail without this.
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .build();
    }
}
