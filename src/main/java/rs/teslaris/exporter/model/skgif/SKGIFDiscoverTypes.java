package rs.teslaris.exporter.model.skgif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;

public class SKGIFDiscoverTypes {

    @Builder
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ApiDiscoveryResponse(
        List<Object> context,

        ApiInfo api,

        ServerInfo server,

        List<CollectionInfo> collections,

        AuthenticationInfo authentication,

        PaginationInfo pagination,

        Map<String, Object> extensions
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ApiInfo(
        String name,
        String version,
        String skgIfVersion,
        String description,
        String documentation,
        String openapi,
        String specification,
        ImplementationInfo implementation,
        ContactInfo contact,
        LicenseInfo license
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ImplementationInfo(
        String name,
        String version,
        String url
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ContactInfo(
        String email,
        String url
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record LicenseInfo(
        String name,
        String url
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ServerInfo(
        String url,
        String environment,
        String uptime,
        @JsonProperty("last_updated") ZonedDateTime lastUpdated
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record CollectionInfo(
        @JsonProperty("@type") String type,
        String name,
        String path,
        @JsonProperty("entity_type") String entityType,
        String description,
        @JsonProperty("total_items") Integer totalItems,
        @JsonProperty("supported_filters") List<String> supportedFilters,
        PaginationOptions pagination,
        List<String> sorting
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record PaginationOptions(
        @JsonProperty("default_page_size") Integer defaultPageSize,
        @JsonProperty("max_page_size") Integer maxPageSize,
        Boolean supported
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record AuthenticationInfo(
        Boolean required,
        List<AuthMethod> methods,
        RateLimitInfo rateLimits
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record AuthMethod(
        String type,
        String name,
        String in,
        String description,
        String authorizationUrl,
        String tokenUrl,
        Map<String, String> scopes
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record RateLimitInfo(
        RateLimit anonymous,
        RateLimit authenticated
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record RateLimit(
        Integer requestsPerHour,
        Integer requestsPerDay
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record PaginationInfo(
        Integer defaultPageSize,
        Integer maxPageSize,
        String strategy,
        Map<String, String> parameters
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record FormatsInfo(
        String defaultFormat,
        List<String> supported
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record TeslaRISExtensions(
        String version,
        List<String> modules,
        List<String> harvestingSources,
        List<String> compliance
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Builder
    public record HealthCheckResponse(
        String status,
        ZonedDateTime timestamp,
        ApiVersionInfo api,
        ImplementationStatus implementation,
        String uptime,
        String responseTime,
        ComponentStatus database,
        Map<String, String> endpoints,
        MetricsInfo metrics,
        List<IssueInfo> issues
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ApiVersionInfo(
        String name,
        String version,
        String skgIfVersion
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ImplementationStatus(
        String name,
        String version,
        String build,
        String sha
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ComponentStatus(
        String status,
        String type,
        String health,
        String latency,
        String availableSpace
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record MetricsInfo(
        Integer requestsToday,
        Integer requestsThisHour,
        String averageResponseTime,
        String errorRate
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record IssueInfo(
        String component,
        String status,
        String message,
        ZonedDateTime since
    ) {
    }
}
