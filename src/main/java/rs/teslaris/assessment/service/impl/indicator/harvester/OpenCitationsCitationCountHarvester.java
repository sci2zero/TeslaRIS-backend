package rs.teslaris.assessment.service.impl.indicator.harvester;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class OpenCitationsCitationCountHarvester implements DocumentMetricHarvester {

    private final RestTemplateProvider restTemplateProvider;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    @Override
    public Optional<DocumentMetricResult> harvest(String doi) {
        try {
            String url =
                "https://opencitations.net/index/api/v2/citation-count/doi:" + doi;

            ResponseEntity<String> response =
                restTemplateProvider.provideRestTemplate().getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                return Optional.empty();
            }

            OpenCitationsEntry[] result =
                objectMapper.readValue(response.getBody(), OpenCitationsEntry[].class);

            if (result.length == 0) {
                return Optional.empty();
            }

            return Optional.of(
                new DocumentMetricResult(MetricType.CITATION_COUNT, result[0].count())
            );
        } catch (Exception e) {
            log.error("Error harvesting OpenCitations for DOI {}: {}",
                doi,
                e.getMessage());

            return Optional.empty();
        }
    }

    public record OpenCitationsEntry(
        Integer count
    ) {
    }
}
