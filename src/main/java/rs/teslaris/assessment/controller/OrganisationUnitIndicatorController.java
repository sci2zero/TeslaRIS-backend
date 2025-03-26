package rs.teslaris.assessment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.service.interfaces.OrganisationUnitIndicatorService;

@RestController
@RequestMapping("/api/assessment/organisation-unit-indicator")
@RequiredArgsConstructor
public class OrganisationUnitIndicatorController {

    private final OrganisationUnitIndicatorService organisationUnitIndicatorService;


    @GetMapping("/{organisationUnitId}")
    public Object getOrganisationUnitIndicators(HttpServletRequest request,
                                                @PathVariable Integer organisationUnitId,
                                                @RequestHeader(value = "Authorization", required = false)
                                                String bearerToken,
                                                @CookieValue(value = "jwt-security-fingerprint", required = false)
                                                String fingerprintCookie) {
        return EntityIndicatorController.fetchIndicators(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> organisationUnitIndicatorService.getIndicatorsForOrganisationUnit(
                organisationUnitId,
                accessLevel)
        );
    }
}
