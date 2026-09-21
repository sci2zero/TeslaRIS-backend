package rs.teslaris.core.service.impl;

import jakarta.persistence.EntityManager;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.HealthCheckService;
import software.amazon.awssdk.services.s3.S3Client;

@Service
@RequiredArgsConstructor
public class HealthCheckServiceImpl implements HealthCheckService {

    private final EntityManager entityManager;

    private final ElasticsearchOperations elasticsearchOperations;

    private final MongoTemplate mongoTemplate;

    private final S3Client s3Client;

    private final JavaMailSenderImpl javaMailSender;


    public Map<String, String> checkPostgres() {
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    public Map<String, String> checkElasticsearch() {
        try {
            elasticsearchOperations.indexOps(IndexCoordinates.of("health-check")).exists();
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    public Map<String, String> checkMongo() {
        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    public Map<String, String> checkS3() {
        try {
            s3Client.listBuckets();
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    public Map<String, String> checkMail() {
        try {
            javaMailSender.testConnection(); // not always supported
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }
}
