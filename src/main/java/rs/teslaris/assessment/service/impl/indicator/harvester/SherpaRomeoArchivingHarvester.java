package rs.teslaris.assessment.service.impl.indicator.harvester;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class SherpaRomeoArchivingHarvester implements DocumentMetricHarvester {

//    @Value("${sherpa.romeo.api-key}")
//    private String apiKey;

    private final RestTemplateProvider restTemplateProvider;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    @Override
    public Optional<DocumentMetricResult> harvest(String doi) {
        try {
            String url = String.format(
                "https://v2.sherpa.ac.uk/cgi/retrieve-by-id?item-type=publication&identifier=%s&api-key=%s&format=Json",
                doi
            );

            ResponseEntity<String> response =
                restTemplateProvider.provideRestTemplate().getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                return Optional.empty();
            }

            SherpaRomeoResponse result = objectMapper.readValue(
                response.getBody(),
                SherpaRomeoResponse.class
            );

            boolean archivingAllowed =
                Objects.nonNull(result.items()) && !result.items().isEmpty();

            return Optional.of(
                new DocumentMetricResult(
                    MetricType.SELF_ARCHIVING_ALLOWED,
                    archivingAllowed ? 1 : 0
                )
            );
        } catch (Exception e) {
            log.error("Error harvesting Sherpa Romeo for DOI {}: {}", doi, e.getMessage());
            return Optional.empty();
        }
    }

    public record SherpaRomeoResponse(
        List<Object> items
    ) {
    }
}
