package rs.teslaris.assessment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.service.interfaces.PersonIndicatorService;
import rs.teslaris.core.annotation.Traceable;

@RestController
@RequestMapping("/api/assessment/person-indicator")
@RequiredArgsConstructor
@Traceable
public class PersonIndicatorController {

    private final PersonIndicatorService personIndicatorService;


    @GetMapping("/{personId}")
    public Object getPersonIndicators(HttpServletRequest request,
                                      @PathVariable Integer personId,
                                      @RequestHeader(value = "Authorization", required = false)
                                      String bearerToken,
                                      @CookieValue(value = "jwt-security-fingerprint", required = false)
                                      String fingerprintCookie) {
        return EntityIndicatorController.fetchIndicators(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> personIndicatorService.getIndicatorsForPerson(personId,
                accessLevel)
        );
    }
}
