package rs.teslaris.core.importer.utility.scopus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ScopusImportUtility {

    public static Map<String, String> headers = new HashMap<>();

    private static boolean authenticated = false;


    private boolean authenticate() {
        if (authenticated) {
            return true;
        }

        var url = "https://api.elsevier.com/authenticate?platform=SCOPUS";
        var restTemplate = constructRestTemplate();

        var requestHeaders = new HttpHeaders();
        headers.forEach(requestHeaders::add);

        ResponseEntity<String> responseEntity;
        try {
            responseEntity =
                restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(requestHeaders),
                    String.class);
        } catch (HttpClientErrorException e) {
            log.error("Exception occurred: {}", e.getMessage());
            return false;
        }

        if (responseEntity.getStatusCode() == HttpStatus.MULTIPLE_CHOICES) {
            var objectMapper = new ObjectMapper();
            try {
                var response =
                    objectMapper.readValue(responseEntity.getBody(), AuthenticateResponse.class);
                var idString = response.pathChoices().choice().get(1).id();
                var choice = Integer.parseInt(idString);
                var isSuccess = getAuthtoken(choice, headers);
                if (isSuccess) {
                    authenticated = true;
                }
            } catch (HttpClientErrorException | JsonProcessingException e) {
                log.error("Exception occurred: {}", e.getMessage());
            }
        } else if (responseEntity.getStatusCode() == HttpStatus.OK) {
            authenticated = setAuthToken(responseEntity);
        } else {
            log.error("Exception occurred: {}", responseEntity.getStatusCode());
        }

        return authenticated;
    }

    private boolean getAuthtoken(int choice, Map<String, String> headers) {
        var url = "https://api.elsevier.com/authenticate?platform=SCOPUS&choice=" + choice;
        var restTemplate = constructRestTemplate();

        var requestHeaders = new HttpHeaders();
        headers.forEach(requestHeaders::add);

        ResponseEntity<String> responseEntity;
        try {
            responseEntity =
                restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(requestHeaders),
                    String.class);
        } catch (HttpClientErrorException e) {
            log.error("Exception occurred during auth token fetching: {}", e.getMessage());
            return false;
        }

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return setAuthToken(responseEntity);
        } else {
            log.error("Exception occurred during auth token fetching, status code: {}",
                responseEntity.getStatusCode());
        }

        return false;
    }

    public List<ScopusSearchResponse> getDocumentsByAuthor(String authorID,
                                                           Integer startYear,
                                                           Integer endYear) {
        var retVal = new ArrayList<ScopusSearchResponse>();
        if (authenticate()) {
            var restTemplate = constructRestTemplate();
            var objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            for (int i = startYear; i <= endYear; i++) {
                var url =
                    "https://api.elsevier.com/content/search/scopus?query=AU-ID(" + authorID +
                        ")&count=25&date=" + i + "&view=COMPLETE";
                var response =
                    getDocumentsByQuery(url, ScopusSearchResponse.class, restTemplate,
                        objectMapper);
                if (Objects.nonNull(response)) {
                    retVal.add(response);
                    var numberOfDocumentsInYear =
                        Integer.parseInt(response.searchResults().totalResults());

                    for (int j = 25; j < numberOfDocumentsInYear; j += 25) {
                        var urlYear =
                            "https://api.elsevier.com/content/search/scopus?query=AU-ID(" +
                                authorID + ")&start=" + j + "&count=25&date=" + i +
                                "&view=COMPLETE";
                        var responseYear =
                            getDocumentsByQuery(urlYear, ScopusSearchResponse.class, restTemplate,
                                objectMapper);
                        if (Objects.nonNull(responseYear)) {
                            retVal.add(responseYear);
                        }
                    }
                }
            }
        }
        return retVal;
    }

    @Nullable
    public AbstractDataResponse getAbstractData(String scopusId) {
        if (authenticate()) {
            var url =
                "https://api.elsevier.com/content/abstract/scopus_id/" + scopusId + "?view=FULL";
            var restTemplate = constructRestTemplate();
            var objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return getDocumentsByQuery(url, AbstractDataResponse.class, restTemplate, objectMapper);
        }
        return null;
    }

    @Nullable
    private <T> T getDocumentsByQuery(String query, Class<T> responseType,
                                      RestTemplate restTemplate, ObjectMapper objectMapper) {
        var requestHeaders = new HttpHeaders();
        headers.forEach(requestHeaders::add);

        ResponseEntity<String> responseEntity;
        try {
            responseEntity =
                restTemplate.exchange(query, HttpMethod.GET, new HttpEntity<>(requestHeaders),
                    String.class);
        } catch (HttpClientErrorException e) {
            log.error("Exception occurred during document fetching: {}", e.getMessage());
            return null;
        }

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            try {
                return objectMapper.readValue(responseEntity.getBody(), responseType);
            } catch (JsonProcessingException e) {
                log.error("Exception occurred during document deserialization: {}", e.getMessage());
            }
        } else {
            log.error(
                "Document fetching failed with status code: " + responseEntity.getStatusCode());
        }
        return null;
    }

    private boolean setAuthToken(ResponseEntity<String> responseEntity) {
        var objectMapper = new ObjectMapper();
        try {
            var response =
                objectMapper.readValue(responseEntity.getBody(), AuthTokenResponse.class);
            var authtoken = response.authToken().authtoken();
            headers.put("X-ELS-Authtoken", authtoken);
            return true;
        } catch (HttpClientErrorException | JsonProcessingException e) {
            log.error("Exception occurred during auth token deserialization: {}",
                e.getMessage());
        }

        return false;
    }

    private RestTemplate constructRestTemplate() {
        var httpClient = HttpClients.custom()
            .setProxy(new HttpHost("http", "proxy.uns.ac.rs", 8080))
            .build();

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    private record AuthenticateResponse(
        @JsonProperty("pathChoices") PathChoices pathChoices
    ) {
    }

    private record PathChoices(
        @JsonProperty("choice") List<Choice> choice
    ) {
    }

    private record Choice(
        @JsonProperty("@id") String id
    ) {
    }

    private record AuthTokenResponse(
        @JsonProperty("authenticate-response") AuthToken authToken
    ) {
    }

    private record AuthToken(
        @JsonProperty("@choice") String choice,
        @JsonProperty("@type") String type,
        @JsonProperty("authtoken") String authtoken
    ) {
    }

    public record ScopusSearchResponse(
        @JsonProperty("search-results") SearchResults searchResults
    ) {
    }

    public record SearchResults(
        @JsonProperty("opensearch:totalResults") String totalResults,
        @JsonProperty("opensearch:startIndex") String startIndex,
        @JsonProperty("opensearch:itemsPerPage") String itemsPerPage,
        @JsonProperty("opensearch:Query") OpenSearchQuery query,
        @JsonProperty("link") List<Link> links,
        @JsonProperty("entry") List<Entry> entries
    ) {
    }

    public record OpenSearchQuery(
        @JsonProperty("@role") String role,
        @JsonProperty("@searchTerms") String searchTerms,
        @JsonProperty("@startPage") String startPage
    ) {
    }

    public record Link(
        @JsonProperty("@_fa") boolean fa,
        @JsonProperty("@ref") String ref,
        @JsonProperty("@href") String href,
        @JsonProperty("@type") String type
    ) {
    }

    public record Entry(
        @JsonProperty("@_fa") boolean fa,
        @JsonProperty("link") List<Link> links,
        @JsonProperty("prism:url") String url,
        @JsonProperty("dc:identifier") String identifier,
        @JsonProperty("eid") String eid,
        @JsonProperty("dc:title") String title,
        @JsonProperty("dc:creator") String creator,
        @JsonProperty("prism:publicationName") String publicationName,
        @JsonProperty("prism:issn") String issn,
        @JsonProperty("prism:eIssn") String eIssn,
        @JsonProperty("prism:pageRange") String pageRange,
        @JsonProperty("prism:coverDate") String coverDate,
        @JsonProperty("prism:coverDisplayDate") String coverDisplayDate,
        @JsonProperty("prism:doi") String doi,
        @JsonProperty("dc:description") String description,
        @JsonProperty("citedby-count") String citedByCount,
        @JsonProperty("affiliation") List<Affiliation> affiliations,
        @JsonProperty("prism:aggregationType") String aggregationType,
        @JsonProperty("subtype") String subtype,
        @JsonProperty("subtypeDescription") String subtypeDescription,
        @JsonProperty("author-count") AuthorCount authorCount,
        @JsonProperty("author") List<Author> authors,
        @JsonProperty("authkeywords") String authKeywords,
        @JsonProperty("source-id") String sourceId,
        @JsonProperty("fund-acr") String fundAcr,
        @JsonProperty("fund-no") String fundNo,
        @JsonProperty("fund-sponsor") String fundSponsor,
        @JsonProperty("openaccess") String openAccess,
        @JsonProperty("openaccessFlag") boolean openAccessFlag
    ) {
    }

    public record Affiliation(
        @JsonProperty("@_fa") boolean fa,
        @JsonProperty("affiliation-url") String affiliationUrl,
        @JsonProperty("afid") String afid,
        @JsonProperty("affilname") String affilName,
        @JsonProperty("affiliation-city") String affiliationCity,
        @JsonProperty("affiliation-country") String affiliationCountry
    ) {
    }

    public record AuthorCount(
        @JsonProperty("@limit") String limit,
        @JsonProperty("@total") String total,
        @JsonProperty("$") String count
    ) {
    }

    public record Author(
        @JsonProperty("@_fa") boolean fa,
        @JsonProperty("@seq") String seq,
        @JsonProperty("author-url") String authorUrl,
        @JsonProperty("authid") String authId,
        @JsonProperty("authname") String authName,
        @JsonProperty("surname") String surname,
        @JsonProperty("given-name") String givenName,
        @JsonProperty("initials") String initials,
        @JsonProperty("afid") List<AffiliationId> afid
    ) {
    }

    public record AffiliationId(
        @JsonProperty("@_fa") boolean fa,
        @JsonProperty("$") String id
    ) {
    }

    public record AbstractDataResponse(
        @JsonProperty("abstracts-retrieval-response") AbstractRetrievalResponse abstractRetrievalResponse
    ) {
    }

    public record AbstractRetrievalResponse(
        @JsonProperty("coredata") CoreData coreData
    ) {
    }

    public record CoreData(
        @JsonProperty("dc:title") String title,
        @JsonProperty("dc:creator") String creator,
        @JsonProperty("prism:publicationName") String publicationName,
        @JsonProperty("prism:coverDate") String coverDate,
        @JsonProperty("citedby-count") String citedByCount
    ) {
    }
}
