package rs.teslaris.importer.utility.webofscience;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import rs.teslaris.core.util.session.RestTemplateProvider;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebOfScienceImportUtility {

    private static final String BASE_URL =
        "https://api.clarivate.com/apis/wos-starter/v1/documents";

    private final RestTemplateProvider restTemplateProvider;

    private final ObjectMapper objectMapper;

    @Value("${wos.api.key}")
    private String apiKey;


    public List<WosPublication> getPublicationsForAuthors(List<String> wosIds, String dateFrom,
                                                          String dateTo) {
        var restTemplate = restTemplateProvider.provideRestTemplate();
        var query = buildAuthorQuery(wosIds);
        return fetchPublications(query, dateFrom, dateTo, restTemplate);
    }

    public List<WosPublication> getPublicationsForInstitution(String institutionName,
                                                              String dateFrom, String dateTo) {
        var restTemplate = restTemplateProvider.provideRestTemplate();
        var query = "OG=(" + institutionName + ")";
        return fetchPublications(query, dateFrom, dateTo, restTemplate);
    }

    private String buildAuthorQuery(List<String> wosIds) {
        StringBuilder query = new StringBuilder("AI=(");
        for (var researcherId : wosIds) {
            query.append(researcherId);
            if (!researcherId.equals(wosIds.getLast())) {
                query.append(" OR ");
            }
        }

        return query + ")";
    }

    private List<WosPublication> fetchPublications(String query, String dateFrom,
                                                   String dateTo, RestTemplate restTemplate) {
        int page = 1;
        int limit = 50;
        var allPublications = new ArrayList<WosPublication>();

        while (true) {
            var url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("db", "WOK") // Web of Knowledge represents the union of all WoS DBs
                .queryParam("q", query + " AND DT=(Article OR Proceedings Paper OR Meeting)")
                .queryParam("limit", limit)
                .queryParam("page", page)
                .queryParam("publishTimeSpan", dateFrom + "+" + dateTo)
                .build(false)
                .toUriString();

            var headers = new HttpHeaders();
            headers.set("X-ApiKey", apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
                );
            } catch (ResourceAccessException e) {
                log.warn(
                    "Unable to access WoS service while performing harvest, aborting... Reason: {}",
                    e.getMessage());
                return allPublications;
            }

            if (response.getStatusCode().is2xxSuccessful() && Objects.nonNull(response.getBody())) {
                WoSResults results;
                try {
                    results = objectMapper.readValue(response.getBody(), WoSResults.class);
                } catch (JsonProcessingException e) {
                    log.error("Unable to parse WoS response. Reason: {}", e.getMessage());
                    break;
                }
                List<WosPublication> records = results.hits();
                allPublications.addAll(records);

                if (results.metadata().page() * results.metadata().limit() >=
                    results.metadata().total()) {
                    break;
                }

                page++;
            } else {
                log.error("Unable to parse WoS response. Response status: {}",
                    response.getStatusCode());
                break;
            }
        }

        return allPublications;
    }

    public record WoSResults(
        Metadata metadata,
        List<WosPublication> hits
    ) {
    }

    public record Metadata(
        int total,
        int page,
        int limit
    ) {
    }

    public record WosPublication(
        String uid,
        String title,
        List<String> types,
        List<String> sourceTypes,
        Source source,
        Names names,
        Links links,
        List<Citation> citations,
        Identifiers identifiers,
        Keywords keywords
    ) {
    }

    public record Source(
        String sourceTitle,
        int publishYear,
        String volume,
        Pages pages
    ) {
    }

    public record Pages(
        String range,
        String begin,
        String end,
        int count
    ) {
    }

    public record Names(
        List<Author> authors,
        List<BookEditor> bookEditors
    ) {
    }

    public record Author(
        String displayName,
        String wosStandard,
        String researcherId
    ) {
    }

    public record BookEditor(
        String displayName
    ) {
    }

    public record Links(
        String record,
        String references,
        String related
    ) {
    }

    public record Citation(
        String db,
        int count
    ) {
    }

    public record Identifiers(
        String doi,
        String issn,
        String eissn,
        String isbn,
        String eisbn
    ) {
    }

    public record Keywords(
        List<String> authorKeywords
    ) {
    }
}
