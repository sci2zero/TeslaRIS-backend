package rs.teslaris.assessment.service.impl.indicator.harvester;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.service.impl.indicator.DocumentMetricHarvester;
import rs.teslaris.assessment.service.impl.indicator.DocumentMetricResult;
import rs.teslaris.assessment.service.impl.indicator.MetricType;
import rs.teslaris.core.util.session.RestTemplateProvider;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnpaywallOpenAccessHarvester implements DocumentMetricHarvester {

    private final RestTemplateProvider restTemplateProvider;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    @Value("${unpaywall.email}")
    private String email;

    @Override
    public Optional<DocumentMetricResult> harvest(String doi) {
        try {
            String url = String.format(
                "https://api.unpaywall.org/v2/%s?email=%s", doi, email
            );

            ResponseEntity<String> response =
                restTemplateProvider.provideRestTemplate()
                    .getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                return Optional.empty();
            }

            UnpaywallResponse result =
                objectMapper.readValue(response.getBody(), UnpaywallResponse.class
                );

            return Optional.of(
                new DocumentMetricResult(
                    MetricType.OPEN_ACCESS,
                    Boolean.TRUE.equals(result.isOa()) ? 1 : 0
                )
            );
        } catch (Exception e) {
            log.error(
                "Error harvesting Unpaywall for DOI {}: {}",
                doi,
                e.getMessage()
            );

            return Optional.empty();
        }
    }

    public record UnpaywallResponse(
        Boolean is_oa
    ) {
        public Boolean isOa() {
            return is_oa;
        }
    }
}
