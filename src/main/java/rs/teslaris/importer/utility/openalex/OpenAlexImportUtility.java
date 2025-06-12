package rs.teslaris.importer.utility.openalex;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class OpenAlexImportUtility {

    private final RestTemplate restTemplate;

    @Value("${proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${proxy.host:}")
    private String proxyHost;

    @Value("${proxy.port:0}")
    private int proxyPort;

    @Value("${proxy.type:HTTP}") // HTTP or SOCKS
    private String proxyType;


    @Autowired
    public OpenAlexImportUtility(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
            .requestFactory(this::createRequestFactory)
            .build();
    }

    public List<OpenAlexPublication> getPublicationsForAuthor(String openAlexId, String dateFrom,
                                                              String dateTo,
                                                              Boolean institutionLevelHarvest) {
        List<OpenAlexPublication> allResults = new ArrayList<>();
        var baseUrl = "https://api.openalex.org/works?per-page=100" +
            "&filter=" + (institutionLevelHarvest ? "author.id:" : "institution.id:") + openAlexId +
            ",from_publication_date:" + dateFrom +
            ",to_publication_date:" + dateTo;

        var cursor = "*";  // start with initial cursor
        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            while (Objects.nonNull(cursor)) {
                String paginatedUrl =
                    baseUrl + "&cursor=" + URLEncoder.encode(cursor, StandardCharsets.UTF_8);
                ResponseEntity<String> responseEntity =
                    this.restTemplate.getForEntity(paginatedUrl, String.class);

                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    break;
                }

                var results =
                    objectMapper.readValue(responseEntity.getBody(), OpenAlexResults.class);
                if (Objects.nonNull(results.results())) {
                    allResults.addAll(results.results());
                }

                cursor = Objects.nonNull(results.meta()) ? results.meta().nextCursor() : null;
            }
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching OpenAlex works: {}", e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("JSON parsing error: {}", e.getMessage());
        }

        return allResults;
    }

    private SimpleClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        if (proxyEnabled && proxyHost != null && proxyPort > 0) {
            Proxy.Type type =
                "SOCKS".equalsIgnoreCase(proxyType) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            Proxy proxy = new Proxy(type, new InetSocketAddress(proxyHost, proxyPort));
            factory.setProxy(proxy);
        }

        return factory;
    }

    public record OpenAlexResults(
        @JsonProperty("results") List<OpenAlexPublication> results,
        Meta meta
    ) {
    }

    public record Meta(
        @JsonProperty("next_cursor")
        String nextCursor
    ) {
    }

    public record OpenAlexPublication(
        String id,
        String doi,
        String title,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("publication_year") Integer publicationYear,
        @JsonProperty("publication_date") String publicationDate,
        Ids ids,
        String language,
        @JsonProperty("primary_location") PrimaryLocation primaryLocation,
        String type,
        @JsonProperty("type_crossref") String typeCrossref,
        @JsonProperty("indexed_in") List<String> indexedIn,
        @JsonProperty("open_access") OpenAccess openAccess,
        List<Authorship> authorships,
        @JsonProperty("is_retracted") Boolean isRetracted,
        @JsonProperty("is_paratext") Boolean isParatext,
        List<Keyword> keywords,
        @JsonProperty("referenced_works") List<String> referencedWorks,
        @JsonProperty("related_works") List<String> relatedWorks,
        @JsonProperty("updated_date") String updatedDate,
        @JsonProperty("created_date") String createdDate
    ) {
        public record Ids(
            String openalex,
            String doi,
            String mag
        ) {
        }

        public record PrimaryLocation(
            @JsonProperty("is_oa") Boolean isOa,
            @JsonProperty("landing_page_url") String landingPageUrl,
            @JsonProperty("pdf_url") String pdfUrl,
            Source source
        ) {
            public record Source(
                String id,
                @JsonProperty("display_name") String displayName,
                @JsonProperty("issn_l") String issnL,
                List<String> issn,
                @JsonProperty("is_oa") Boolean isOa,
                @JsonProperty("is_in_doaj") Boolean isInDoaj,
                @JsonProperty("is_indexed_in_scopus") Boolean isIndexedInScopus,
                @JsonProperty("is_core") Boolean isCore,
                @JsonProperty("host_organization") String hostOrganization,
                @JsonProperty("host_organization_name") String hostOrganizationName,
                @JsonProperty("host_organization_lineage") List<String> hostOrganizationLineage,
                @JsonProperty("host_organization_lineage_names") List<String> hostOrganizationLineageNames,
                String type
            ) {
            }
        }

        public record OpenAccess(
            @JsonProperty("is_oa") Boolean isOa,
            @JsonProperty("oa_status") String oaStatus,
            @JsonProperty("oa_url") String oaUrl,
            @JsonProperty("any_repository_has_fulltext") Boolean anyRepositoryHasFulltext
        ) {
        }

        public record Authorship(
            @JsonProperty("author_position") String authorPosition,
            Author author,
            List<Institution> institutions,
            List<String> countries,
            @JsonProperty("is_corresponding") Boolean isCorresponding,
            @JsonProperty("raw_author_name") String rawAuthorName,
            @JsonProperty("raw_affiliation_strings") List<String> rawAffiliationStrings,
            List<Affiliation> affiliations
        ) {
            public record Author(
                String id,
                @JsonProperty("display_name") String displayName,
                String orcid
            ) {
            }

            public record Institution(
                String id,
                @JsonProperty("display_name") String displayName,
                String ror,
                @JsonProperty("country_code") String countryCode,
                String type,
                List<String> lineage
            ) {
            }

            public record Affiliation(
                @JsonProperty("raw_affiliation_string") String rawAffiliationString,
                @JsonProperty("institution_ids") List<String> institutionIds
            ) {
            }
        }

        public record Keyword(
            String id,
            @JsonProperty("display_name") String displayName,
            Double score
        ) {
        }
    }
}
