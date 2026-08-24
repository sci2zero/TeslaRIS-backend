package rs.teslaris.revisioner.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.revisioner.dto.EntityTypeQualityDTO;
import rs.teslaris.revisioner.service.interfaces.RepositoryAnalyticsService;

@RestController
@RequestMapping("/api/repository-analytics")
@RequiredArgsConstructor
public class RepositoryAnalyticsController {

    private final RepositoryAnalyticsService repositoryAnalyticsService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


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
