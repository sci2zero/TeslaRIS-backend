package rs.teslaris.assessment.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.dto.IFTableResponseDTO;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.service.interfaces.indicator.PublicationSeriesIndicatorService;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/publication-series-indicator")
@RequiredArgsConstructor
@Traceable
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
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleLoadingOfPublicationSeriesIndicators(@RequestParam("timestamp")
                                                             LocalDateTime timestamp,
                                                             @RequestParam("source")
                                                             EntityIndicatorSource source,
                                                             @RequestHeader("Authorization")
                                                             String bearerToken) {
        publicationSeriesIndicatorService.scheduleIndicatorLoading(timestamp, source,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PostMapping("/schedule-if5-jci-compute")
    @Idempotent
    @PreAuthorize("hasAuthority('SCHEDULE_TASK')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleIF5AndJCIRankCompute(@RequestParam("timestamp")
                                             LocalDateTime timestamp,
                                             @RequestParam("classificationYears")
                                             List<Integer> classificationYears,
                                             @RequestHeader("Authorization")
                                             String bearerToken) {
        publicationSeriesIndicatorService.scheduleIF5AndJCIRankComputation(timestamp,
            classificationYears, tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/if-table/{publicationSeriesId}")
    public IFTableResponseDTO scheduleIF5RankCompute(
        @PathVariable Integer publicationSeriesId, @RequestParam("fromYear") Integer fromYear,
        @RequestParam("toYear") Integer toYear) {
        return publicationSeriesIndicatorService.getIFTableContent(publicationSeriesId, fromYear,
            toYear);
    }
}
