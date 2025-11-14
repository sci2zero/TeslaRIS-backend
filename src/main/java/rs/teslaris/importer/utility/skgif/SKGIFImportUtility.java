package rs.teslaris.importer.utility.skgif;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.exporter.model.skgif.SKGIFSingleResponse;

@Component
@Slf4j
public class SKGIFImportUtility {

    private static RestTemplateProvider restTemplateProvider;


    @Autowired
    public SKGIFImportUtility(RestTemplateProvider restTemplateProvider) {
        SKGIFImportUtility.restTemplateProvider = restTemplateProvider;
    }

    public static <T> Optional<List<T>> fetchEntityFromExternalGraph(String entityId,
                                                                     String set,
                                                                     Class<T> entityClass,
                                                                     String baseUrl) {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String fetchUrl = baseUrl + "/" + set + "/" + entityId;
        ResponseEntity<String> responseEntity =
            restTemplateProvider.provideRestTemplate().getForEntity(fetchUrl, String.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return Optional.empty();
        }

        try {
            SKGIFSingleResponse<?> result = objectMapper.readValue(
                responseEntity.getBody(),
                objectMapper.getTypeFactory()
                    .constructParametricType(SKGIFSingleResponse.class, entityClass)
            );

            if (result.getGraph().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of((List<T>) result.getGraph());
        } catch (HttpClientErrorException e) {
            log.error("HTTP error for SKG-IF {} ID {}: {}", entityClass.getName(), entityId,
                e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("JSON parsing error for SKG-IF {} ID {}: {}", entityClass.getName(), entityId,
                e.getMessage());
        } catch (ResourceAccessException e) {
            log.error("SKG-IF {} source is unreachable for ID {}: {}", entityClass.getName(),
                entityId, e.getMessage());
        }

        return Optional.empty();
    }
}
