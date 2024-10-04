package rs.teslaris.core.assessment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.assessment.service.interfaces.PublicationSeriesIndicatorService;

@RestController
@RequestMapping("/api/assessment/publication-series-indicator")
@RequiredArgsConstructor
public class PublicationSeriesIndicatorController {

    private final PublicationSeriesIndicatorService publicationSeriesIndicatorService;


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
}
