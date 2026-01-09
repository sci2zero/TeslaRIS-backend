package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.MongoDatabase;
import io.minio.MinioClient;
import io.minio.messages.Bucket;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import rs.teslaris.core.service.impl.HealthCheckServiceImpl;

@SpringBootTest
public class HealthCheckServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private MinioClient minioClient;

    @Mock
    private JavaMailSenderImpl javaMailSender;

    @Mock
    private Query query;

    @Mock
    private IndexOperations indexOperations;

    @Mock
    private MongoDatabase mongoDatabase;

    @InjectMocks
    private HealthCheckServiceImpl healthCheckService;


    @Test
    void checkPostgresShouldReturnUpWhenConnectionSuccessful() {
        // Given
        when(entityManager.createNativeQuery("SELECT 1")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);

        // When
        var result = healthCheckService.checkPostgres();

        // Then
        assertEquals("UP", result.get("status"));
        verify(entityManager).createNativeQuery("SELECT 1");
        verify(query).getSingleResult();
    }

    @Test
    void checkPostgresShouldReturnDownWhenConnectionFails() {
        // Given
        when(entityManager.createNativeQuery("SELECT 1")).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new RuntimeException("Connection refused"));

        // When
        var result = healthCheckService.checkPostgres();

        // Then
        assertEquals("DOWN", result.get("status"));
        assertNotNull(result.get("error"));
        assertEquals("Connection refused", result.get("error"));
    }

    @Test
    void checkElasticsearchShouldReturnUpWhenConnectionSuccessful() {
        // Given
        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class))).thenReturn(
            indexOperations);
        when(indexOperations.exists()).thenReturn(true);

        // When
        var result = healthCheckService.checkElasticsearch();

        // Then
        assertEquals("UP", result.get("status"));
        verify(indexOperations).exists();
    }

    @Test
    void checkElasticsearchShouldReturnDownWhenConnectionFails() {
        // Given
        when(elasticsearchOperations.indexOps(any(IndexCoordinates.class))).thenReturn(
            indexOperations);
        when(indexOperations.exists()).thenThrow(new RuntimeException("No nodes available"));

        // When
        var result = healthCheckService.checkElasticsearch();

        // Then
        assertEquals("DOWN", result.get("status"));
        assertNotNull(result.get("error"));
        assertEquals("No nodes available", result.get("error"));
    }

    @Test
    void checkMongoShouldReturnUpWhenConnectionSuccessful() {
        // Given
        when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
        var successDoc = new Document("ok", 1.0);
        when(mongoDatabase.runCommand(any(Document.class))).thenReturn(successDoc);

        // When
        var result = healthCheckService.checkMongo();

        // Then
        assertEquals("UP", result.get("status"));
        verify(mongoTemplate).getDb();
    }

    @Test
    void checkMongoShouldReturnDownWhenConnectionFails() {
        // Given
        when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
        when(mongoDatabase.runCommand(any(Document.class)))
            .thenThrow(new RuntimeException("Connection timeout"));

        // When
        var result = healthCheckService.checkMongo();

        // Then
        assertEquals("DOWN", result.get("status"));
        assertNotNull(result.get("error"));
        assertEquals("Connection timeout", result.get("error"));
    }

    // MinIO Tests
    @Test
    void checkMinioShouldReturnUpWhenConnectionSuccessful() throws Exception {
        // Given
        var bucketList = new ArrayList<Bucket>();
        when(minioClient.listBuckets()).thenReturn(bucketList);

        // When
        var result = healthCheckService.checkMinio();

        // Then
        assertEquals("UP", result.get("status"));
        verify(minioClient).listBuckets();
    }

    @Test
    void checkMinioShouldReturnDownWhenConnectionFails() throws Exception {
        // Given
        when(minioClient.listBuckets()).thenThrow(new RuntimeException("Invalid endpoint"));

        // When
        var result = healthCheckService.checkMinio();

        // Then
        assertEquals("DOWN", result.get("status"));
        assertNotNull(result.get("error"));
        assertEquals("Invalid endpoint", result.get("error"));
    }

    @Test
    void checkMailShouldReturnUpWhenConnectionSuccessful() throws MessagingException {
        // Given
        doNothing().when(javaMailSender).testConnection();

        // When
        var result = healthCheckService.checkMail();

        // Then
        assertEquals("UP", result.get("status"));
        verify(javaMailSender).testConnection();
    }

    @Test
    void checkMailShouldReturnDownWhenConnectionFails() throws MessagingException {
        // Given
        doThrow(new RuntimeException("Mail server unreachable"))
            .when(javaMailSender).testConnection();

        // When
        var result = healthCheckService.checkMail();

        // Then
        assertEquals("DOWN", result.get("status"));
        assertNotNull(result.get("error"));
        assertEquals("Mail server unreachable", result.get("error"));
    }
}
