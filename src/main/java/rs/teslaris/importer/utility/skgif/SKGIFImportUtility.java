package rs.teslaris.importer.utility.skgif;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import rs.teslaris.core.model.skgif.researchproduct.ResearchProduct;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.exporter.model.skgif.SKGIFListResponse;

@Component
@RequiredArgsConstructor
@Slf4j
public class SKGIFImportUtility {

    private final RestTemplateProvider restTemplateProvider;

    private final int PAGE_SIZE = 100;


    public List<ResearchProduct> getPublicationsForAuthors(List<String> internalEntityIds,
                                                           Boolean institutionLevelHarvest,
                                                           String dateFrom,
                                                           String dateTo,
                                                           String baseUrl) {
        var allResults = new ArrayList<ResearchProduct>();
        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (String entityId : internalEntityIds) {
            String harvestUrl = baseUrl + "products?page_size=" + PAGE_SIZE +
                "&filter=" +
                (institutionLevelHarvest ? "relevant_organisations:" : "contributions.by:") +
                entityId +
                "dateFrom=" + dateFrom + "&dateTo=" + dateTo;

            var page = 0;
            var shouldContinue = true;
            try {
                while (shouldContinue) {
                    String paginatedUrl = harvestUrl + "&page=" + page;
                    ResponseEntity<String> responseEntity =
                        restTemplateProvider.provideRestTemplate()
                            .getForEntity(paginatedUrl, String.class);

                    if (responseEntity.getStatusCode() != HttpStatus.OK) {
                        break;
                    }

                    var results =
                        objectMapper.readValue(responseEntity.getBody(), SKGIFListResponse.class);

                    if (Objects.nonNull(results.getResults())) {
                        results.getResults()
                            .forEach(result -> allResults.add((ResearchProduct) result));
                    }

                    shouldContinue = results.getResults().size() == PAGE_SIZE;
                }
            } catch (HttpClientErrorException e) {
                log.error("HTTP error for SKG-IF client ID {}: {}", entityId, e.getMessage());
            } catch (JsonProcessingException e) {
                log.error("JSON parsing error for SKG-IF client ID {}: {}", entityId,
                    e.getMessage());
            } catch (ResourceAccessException e) {
                log.error("SKG-IF client is unreachable for ID {}: {}", entityId, e.getMessage());
            }
        }

        return allResults;
    }
}
