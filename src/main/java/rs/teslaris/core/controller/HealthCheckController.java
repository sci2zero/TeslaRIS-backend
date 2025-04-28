package rs.teslaris.core.controller;

import io.minio.MinioClient;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health-check")
@RequiredArgsConstructor
public class HealthCheckController {

    private final EntityManager entityManager;

    private final ElasticsearchOperations elasticsearchOperations;

    private final MongoTemplate mongoTemplate;

    private final MinioClient minioClient;

    private final JavaMailSenderImpl javaMailSender;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.git.tag:}")
    private String appGitTag;

    @Value("${app.git.commit.hash:}")
    private String appGitCommitHash;

    @Value("${app.git.repo.url:}")
    private String appGitRepoUrl;

    @GetMapping
    @PreAuthorize("hasAuthority('PERFORM_HEALTH_CHECK')")
    public ResponseEntity<Map<String, Object>> performHealthCheck() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("appVersion", appVersion);

        status.put("Postgres", checkPostgres());
        status.put("Elasticsearch", checkElasticsearch());
        status.put("MinIO", checkMinio());
        status.put("Mail Server", checkMail());
        status.put("MongoDB", checkMongo());

        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    private Map<String, String> checkPostgres() {
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    private Map<String, String> checkElasticsearch() {
        try {
            elasticsearchOperations.indexOps(IndexCoordinates.of("health-check")).exists();
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    private Map<String, String> checkMongo() {
        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    private Map<String, String> checkMinio() {
        try {
            minioClient.listBuckets();
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    private Map<String, String> checkMail() {
        try {
            javaMailSender.testConnection(); // not always supported
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> getVersion() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("appVersion", appVersion);
        status.put("appGitRepoUrl", appGitRepoUrl);
        status.put("appGitTag", appGitTag);
        status.put("appGitCommitHash", appGitCommitHash);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }
}
