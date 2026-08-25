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
import rs.teslaris.revisioner.service.interfaces.RepositoryAnalyticsService;

@RestController
@RequestMapping("/api/repository-analytics")
@RequiredArgsConstructor
public class RepositoryAnalyticsController {

    private final RepositoryAnalyticsService repositoryAnalyticsService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


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

    private ResponseEntity<InputStreamResource> serveResponseFile(InputStreamResource report,
                                                                  String fileName) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                StringUtil.contentDisposition(fileName + ".xlsx"))
            .body(report);
    }

    /**
     * @return the unit an institutional editor or a vice dean for science is bound to, or
     * {@code null} for an admin, who sees the whole repository
     */
    @Nullable
    private Integer resolveOrganisationUnitId(String bearerToken) {
        var user = userService.findOne(tokenUtil.extractUserIdFromToken(bearerToken));

        return Objects.nonNull(user.getOrganisationUnit())
            ? user.getOrganisationUnit().getId()
            : null;
    }

    @GetMapping(value = "/dimensions", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public List<DimensionQualityDTO> getQualityByDimension(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestHeader("Authorization") String bearerToken) {
        var user = userService.findOne(tokenUtil.extractUserIdFromToken(bearerToken));

        if (Objects.nonNull(user.getOrganisationUnit())) {
            // User is institutional editor or research information editor (vice dean for science)
            return repositoryAnalyticsService.getQualityByDimension(profileName,
                user.getOrganisationUnit().getId(), assessmentDate);
        }

        // User is admin
        return repositoryAnalyticsService.getQualityByDimension(
            profileName, null, assessmentDate
        );
    }

    @GetMapping(value = "/entity-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public List<EntityTypeQualityDTO> getQualityByEntityType(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestHeader("Authorization") String bearerToken) {
        var user = userService.findOne(tokenUtil.extractUserIdFromToken(bearerToken));

        if (Objects.nonNull(user.getOrganisationUnit())) {
            // User is institutional editor or research information editor (vice dean for science)
            return repositoryAnalyticsService.getQualityByEntityType(profileName,
                user.getOrganisationUnit().getId(), assessmentDate);
        }

        // User is admin
        return repositoryAnalyticsService.getQualityByEntityType(
            profileName, null, assessmentDate
        );
    }
}
