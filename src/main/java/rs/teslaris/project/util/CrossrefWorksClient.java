package rs.teslaris.project.util;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import rs.teslaris.core.util.session.RestTemplateProvider;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrossrefWorksClient {

    private static final String CROSSREF_WORKS_URL = "https://api.crossref.org/works/";

    // When this header is set, Crossref gives access to Polite Pool which increases the rate limit
    // from 5 -> 10 and concurrency limit from 1 -> 3
    private static final String POLITE_POOL_USER_AGENT =
            "TeslaRIS/1.10 (mailto:teslaris@uns.ac.rs)";

    private final RestTemplateProvider restTemplateProvider;

    @Nullable
    public JsonNode fetchWorkMessage(String doi) {
        var url = CROSSREF_WORKS_URL + doi;

        var headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, POLITE_POOL_USER_AGENT);

        var entity = new HttpEntity<>(headers);
        var restTemplate = restTemplateProvider.provideRestTemplate();

        try {
            var response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            var body = response.getBody();

            if (Objects.isNull(body)) {
                return null;
            }

            var message = body.path("message");
            return message.isMissingNode() ? null : message;
        } catch (HttpClientErrorException e) {
            log.warn("Unable to fetch Crossref metadata for DOI: {}. Response code: {}", doi,
                    e.getStatusCode().value());
            return null;
        }
    }
}
