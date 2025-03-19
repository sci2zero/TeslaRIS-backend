package rs.teslaris.core.assessment.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.assessment.dto.IFTableResponseDTO;
import rs.teslaris.core.assessment.model.EntityIndicatorSource;
import rs.teslaris.core.assessment.service.interfaces.PublicationSeriesIndicatorService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/publication-series-indicator")
@RequiredArgsConstructor
public class PublicationSeriesIndicatorController {

    private final PublicationSeriesIndicatorService publicationSeriesIndicatorService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{publicationSeriesId}")
    public Object getPublicationSeriesIndicators(HttpServletRequest request,
                                                 @PathVariable Integer publicationSeriesId,
                                                 @RequestHeader(value = "Authorization", required = false)
                                                 String bearerToken,
                                                 @CookieValue(value = "jwt-security-fingerprint", required = false)
                                                 String fingerprintCookie) {
        return EntityIndicatorController.fetchIndicators(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> publicationSeriesIndicatorService.getIndicatorsForPublicationSeries(
                publicationSeriesId,
                accessLevel)
        );
    }

    @PostMapping("/schedule-load")
    @Idempotent
    @PreAuthorize("hasAuthority('SCHEDULE_TASK')")
    public void scheduleLoadingOfPublicationSeriesIndicators(@RequestParam("timestamp")
                                                             LocalDateTime timestamp,
                                                             @RequestParam("source")
                                                             EntityIndicatorSource source,
                                                             @RequestHeader("Authorization")
                                                             String bearerToken) {
        publicationSeriesIndicatorService.scheduleIndicatorLoading(timestamp, source,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PostMapping("/schedule-if5-compute")
    @Idempotent
    @PreAuthorize("hasAuthority('SCHEDULE_TASK')")
    public void scheduleIF5RankCompute(@RequestParam("timestamp")
                                       LocalDateTime timestamp,
                                       @RequestParam("classificationYears")
                                       List<Integer> classificationYears,
                                       @RequestHeader("Authorization")
                                       String bearerToken) {
        publicationSeriesIndicatorService.scheduleIF5RankComputation(timestamp, classificationYears,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/if-table/{publicationSeriesId}")
    public IFTableResponseDTO scheduleIF5RankCompute(
        @PathVariable Integer publicationSeriesId, @RequestParam("fromYear") Integer fromYear,
        @RequestParam("toYear") Integer toYear) {
        return publicationSeriesIndicatorService.getIFTableContent(publicationSeriesId, fromYear,
            toYear);
    }
}
