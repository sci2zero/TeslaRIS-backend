package rs.teslaris.exporter.util.skgif.builder;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.interfaces.HealthCheckService;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.model.skgif.SKGIFDiscoverTypes;
import rs.teslaris.exporter.util.skgif.OrganisationFilteringUtil;
import rs.teslaris.exporter.util.skgif.PersonFilteringUtil;
import rs.teslaris.exporter.util.skgif.ResearchProductFilteringUtil;
import rs.teslaris.exporter.util.skgif.VenueFilteringUtil;

@Component
@RequiredArgsConstructor
public class SKGIFRecordsFactory {

    private final MongoTemplate mongoTemplate;

    private final HealthCheckService healthCheckService;

    @Value("${app.git.repo.url}")
    private String repoUrl;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.git.commit.hash}")
    private String commitHash;

    @Value("${app.git.tag}")
    private String gitTag;

    @Value("${frontend.application.address}")
    private String appUrl;


    public SKGIFDiscoverTypes.ApiDiscoveryResponse createDefaultDiscovery() {
        return SKGIFDiscoverTypes.ApiDiscoveryResponse.builder()
            .context(List.of(
                "https://w3id.org/skg-if/context/1.1.0/skg-if.json",
                "https://w3id.org/skg-if/context/1.0.0/skg-if-api.json",
                Map.of("@base", appUrl + getPathSeparator(appUrl) + "skg-if/api/")
            ))
            .api(new SKGIFDiscoverTypes.ApiInfo(
                "TeslaRIS SKG-IF API",
                "1.1.0",
                "1.1.0",
                "SKG-IF compliant API for TeslaRIS",
                "https://github.com/sci2zero/TeslaRIS-backend/wiki/SKG%E2%80%90IF-API",
                "", // not supported
                "https://skg-if.github.io/interoperability-framework/",
                new SKGIFDiscoverTypes.ImplementationInfo("TeslaRIS", appVersion,
                    "https://github.com/sci2zero/TeslaRIS-backend"),
                new SKGIFDiscoverTypes.ContactInfo("info@sci2zero.org",
                    "http://sci2zero.org/contact/"),
                new SKGIFDiscoverTypes.LicenseInfo("GPL-3.0 License",
                    "https://www.gnu.org/licenses/gpl-3.0.en.html#license-text")
            ))
            .server(new SKGIFDiscoverTypes.ServerInfo(
                appUrl + getPathSeparator(appUrl) + "skg-if/api/",
                "production", null, null
            ))
            .collections(List.of(
                new SKGIFDiscoverTypes.CollectionInfo(
                    "skgif:Collection", "Products", "/product", "product",
                    "Research products including publications, datasets, software, and other research outputs",
                    (int) mongoTemplate.count(getCollectionQuery(ExportDocument.class, false),
                        ExportDocument.class),
                    ResearchProductFilteringUtil.SUPPORTED_FILTERS,
                    getDefaultPaginationOptions(),
                    Collections.emptyList()
                ),
                new SKGIFDiscoverTypes.CollectionInfo(
                    "skgif:Collection", "Persons", "/person", "person",
                    "Researchers that are managed by this system. Includes affiliations if set.",
                    (int) mongoTemplate.count(getCollectionQuery(ExportPerson.class, false),
                        ExportPerson.class),
                    PersonFilteringUtil.SUPPORTED_FILTERS,
                    getDefaultPaginationOptions(),
                    Collections.emptyList()
                ),
                new SKGIFDiscoverTypes.CollectionInfo(
                    "skgif:Collection", "Organisations", "/organisation", "organisation",
                    "Organisation units including universities, faculties, departments chairs etc.",
                    (int) mongoTemplate.count(
                        getCollectionQuery(ExportOrganisationUnit.class, false),
                        ExportOrganisationUnit.class),
                    OrganisationFilteringUtil.SUPPORTED_FILTERS,
                    getDefaultPaginationOptions(),
                    Collections.emptyList()
                ),
                new SKGIFDiscoverTypes.CollectionInfo(
                    "skgif:Collection", "Venues", "/venue", "venue",
                    "A publishing \"gateway\" used by an agent to make their research products available to others, currently supports journals, conference proceedings and monographs.",
                    (int) mongoTemplate.count(getCollectionQuery(ExportDocument.class, true),
                        ExportDocument.class),
                    VenueFilteringUtil.SUPPORTED_FILTERS,
                    getDefaultPaginationOptions(),
                    Collections.emptyList()
                )
            ))
            .authentication(new SKGIFDiscoverTypes.AuthenticationInfo(false, null, null))
            .pagination(new SKGIFDiscoverTypes.PaginationInfo(
                10, 100, "offset_based",
                Map.of(
                    "page", "page number (0-indexed)",
                    "page_size", "items per page (max 100)"
                )
            ))
            .extensions(Map.of(
                "version", appVersion,
                "modules",
                List.of("core", "import", "export", "assessment", "report", "digitalLibrary"),
                "harvesting_sources",
                List.of(
                    "OAI-PMH", "Scopus", "Web Of Science", "OpenAlex", "Crossref", "DataCite",
                    "SKG-IF", "BibTeX", "EndNote", "RefMan", "REST API"),
                "compliance", List.of("FAIR", "SKG-IF 1.1.0")
            ))
            .build();
    }

    public SKGIFDiscoverTypes.HealthCheckResponse createHealthResponse() {
        var mongoUp = healthCheckService.checkMongo().get("status").equals("UP");

        return SKGIFDiscoverTypes.HealthCheckResponse.builder()
            .status(mongoUp ? "operational" : "nonoperational")
            .timestamp(ZonedDateTime.now())
            .api(new SKGIFDiscoverTypes.ApiVersionInfo("TeslaRIS SKG-IF API", "1.0.0", "1.1.0"))
            .implementation(
                new SKGIFDiscoverTypes.ImplementationStatus("TeslaRIS", "2.0.0", gitTag,
                    commitHash))
            .database(
                new SKGIFDiscoverTypes.ComponentStatus((mongoUp ? "connected" : "disconnected"),
                    "MongoDB", (mongoUp ? "healthy" : "down"),
                    null, null))
            .endpoints(Map.of(
                "products", "/product",
                "persons", "/person",
                "organisations", "/organisation",
                "venues", "/venue",
                "discovery", "/skg-if"
            ))
            .metrics(null) // not collected right now
            .build();
    }

    private String getPathSeparator(String baseUrl) {
        return baseUrl.endsWith("/") ? "" : "/";
    }

    private SKGIFDiscoverTypes.PaginationOptions getDefaultPaginationOptions() {
        return new SKGIFDiscoverTypes.PaginationOptions(10, 100, true);
    }

    private Query getCollectionQuery(Class<?> entityClass, boolean isVenue) {
        var query = new Query();
        query.addCriteria(Criteria.where("deleted").is(false));

        if (entityClass.equals(ExportDocument.class)) {
            var venueTypes =
                List.of(ExportPublicationType.JOURNAL, ExportPublicationType.PROCEEDINGS,
                    ExportPublicationType.MONOGRAPH);
            if (isVenue) {
                query.addCriteria(Criteria.where("type").in(venueTypes));
            } else {
                query.addCriteria(Criteria.where("type").nin(venueTypes));
            }
        }

        return query;
    }
}
