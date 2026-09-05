package rs.teslaris.revisioner.controller;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.revisioner.dto.DimensionQualityDTO;
import rs.teslaris.revisioner.dto.EntityTypeQualityDTO;
import rs.teslaris.revisioner.dto.IssueStatisticsDTO;
import rs.teslaris.revisioner.dto.PublicationCandidateAnalysisDTO;
import rs.teslaris.revisioner.dto.QualityTrendDTO;
import rs.teslaris.revisioner.dto.RepositoryOverviewDTO;
import rs.teslaris.revisioner.service.interfaces.RepositoryAnalyticsService;
import rs.teslaris.revisioner.util.dataquality.TrendGranularity;
import rs.teslaris.revisioner.util.dataquality.TrendMetric;

@RestController
@RequestMapping("/api/repository-analytics")
@RequiredArgsConstructor
public class RepositoryAnalyticsController {

    private final RepositoryAnalyticsService repositoryAnalyticsService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping(value = "/overview", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public RepositoryOverviewDTO getOverview(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestHeader("Authorization") String bearerToken) {
        return repositoryAnalyticsService.getOverview(profileName,
            resolveOrganisationUnitId(bearerToken), assessmentDate);
    }

    @GetMapping(value = "/publication-candidates", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public PublicationCandidateAnalysisDTO getPublicationCandidateAnalysis(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestHeader("Authorization") String bearerToken) {
        return repositoryAnalyticsService.getPublicationCandidateAnalysis(profileName,
            resolveOrganisationUnitId(bearerToken), assessmentDate);
    }

    @GetMapping(value = "/issue-statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public IssueStatisticsDTO getIssueStatistics(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestHeader("Authorization") String bearerToken) {
        return repositoryAnalyticsService.getIssueStatistics(profileName,
            resolveOrganisationUnitId(bearerToken), assessmentDate);
    }

    @GetMapping(value = "/trends", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public QualityTrendDTO getQualityTrend(
        @RequestParam String profileName,
        @RequestParam(defaultValue = "OVERALL_SCORE") TrendMetric metric,
        @RequestParam(defaultValue = "WEEKLY") TrendGranularity granularity,
        @RequestParam(required = false) Integer points,
        @RequestHeader("Authorization") String bearerToken) {
        return repositoryAnalyticsService.getQualityTrend(profileName,
            resolveOrganisationUnitId(bearerToken), metric, granularity, points);
    }

    @GetMapping("/trends/download")
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public ResponseEntity<InputStreamResource> downloadQualityTrend(
        @RequestParam String profileName,
        @RequestParam(defaultValue = "OVERALL_SCORE") TrendMetric metric,
        @RequestParam(defaultValue = "WEEKLY") TrendGranularity granularity,
        @RequestParam(required = false) Integer points,
        @RequestParam(defaultValue = "en") String language,
        @RequestHeader("Authorization") String bearerToken) {
        return serveResponseFile(
            repositoryAnalyticsService.exportQualityTrend(profileName,
                resolveOrganisationUnitId(bearerToken), metric, granularity, points, language),
            "quality-trends");
    }

    @GetMapping("/issue-statistics/download")
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public ResponseEntity<InputStreamResource> downloadIssueStatistics(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestParam(defaultValue = "en") String language,
        @RequestHeader("Authorization") String bearerToken) {
        return serveResponseFile(
            repositoryAnalyticsService.exportIssueStatistics(profileName,
                resolveOrganisationUnitId(bearerToken), assessmentDate, language),
            "issue-statistics");
    }

    @GetMapping("/publication-candidates/download")
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public ResponseEntity<InputStreamResource> downloadPublicationCandidateAnalysis(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestParam(defaultValue = "en") String language,
        @RequestHeader("Authorization") String bearerToken) {
        return serveResponseFile(
            repositoryAnalyticsService.exportPublicationCandidateAnalysis(profileName,
                resolveOrganisationUnitId(bearerToken), assessmentDate, language),
            "publication-candidate-analysis");
    }

    @GetMapping("/overview/download")
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public ResponseEntity<InputStreamResource> downloadOverview(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestParam(defaultValue = "en") String language,
        @RequestHeader("Authorization") String bearerToken) {
        return serveResponseFile(
            repositoryAnalyticsService.exportOverview(profileName,
                resolveOrganisationUnitId(bearerToken), assessmentDate, language),
            "repository-quality-overview");
    }

    @GetMapping("/entity-types/download")
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public ResponseEntity<InputStreamResource> downloadQualityByEntityType(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestParam(defaultValue = "en") String language,
        @RequestHeader("Authorization") String bearerToken) {
        return serveResponseFile(
            repositoryAnalyticsService.exportQualityByEntityType(profileName,
                resolveOrganisationUnitId(bearerToken), assessmentDate, language),
            "quality-by-entity-type");
    }

    @GetMapping("/dimensions/download")
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public ResponseEntity<InputStreamResource> downloadQualityByDimension(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestParam(defaultValue = "en") String language,
        @RequestHeader("Authorization") String bearerToken) {
        return serveResponseFile(
            repositoryAnalyticsService.exportQualityByDimension(profileName,
                resolveOrganisationUnitId(bearerToken), assessmentDate, language),
            "quality-by-dimension");
    }

    @GetMapping(value = "/dimensions", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public List<DimensionQualityDTO> getQualityByDimension(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestHeader("Authorization") String bearerToken) {
        return repositoryAnalyticsService.getQualityByDimension(profileName,
            resolveOrganisationUnitId(bearerToken), assessmentDate
        );
    }

    @GetMapping(value = "/entity-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public List<EntityTypeQualityDTO> getQualityByEntityType(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestHeader("Authorization") String bearerToken) {
        return repositoryAnalyticsService.getQualityByEntityType(
            profileName, resolveOrganisationUnitId(bearerToken), assessmentDate
        );
    }

    private ResponseEntity<InputStreamResource> serveResponseFile(InputStreamResource report,
                                                                  String fileName) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                StringUtil.contentDisposition(fileName + ".xlsx"))
            .body(report);
    }

    @Nullable
    private Integer resolveOrganisationUnitId(String bearerToken) {
        var user = userService.findOne(tokenUtil.extractUserIdFromToken(bearerToken));

        return Objects.nonNull(user.getOrganisationUnit())
            ? user.getOrganisationUnit().getId()
            : null;
    }
}
