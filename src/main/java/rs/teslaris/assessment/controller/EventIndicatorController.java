package rs.teslaris.assessment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.annotation.EntityIndicatorEditCheck;
import rs.teslaris.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.assessment.dto.EventIndicatorDTO;
import rs.teslaris.assessment.service.interfaces.EventIndicatorService;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/event-indicator")
@RequiredArgsConstructor
public class EventIndicatorController {

    private final EventIndicatorService eventIndicatorService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{eventId}")
    public Object getEventIndicators(HttpServletRequest request,
                                     @PathVariable Integer eventId,
                                     @RequestHeader(value = "Authorization", required = false)
                                     String bearerToken,
                                     @CookieValue(value = "jwt-security-fingerprint", required = false)
                                     String fingerprintCookie) {
        return EntityIndicatorController.fetchIndicators(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> eventIndicatorService.getIndicatorsForEvent(eventId,
                accessLevel)
        );
    }

    @PostMapping("/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_EVENT_INDICATORS')")
    @Idempotent
    public EntityIndicatorResponseDTO createEventIndicator(
        @RequestBody EventIndicatorDTO eventIndicatorDTO,
        @RequestHeader("Authorization") String bearerToken) {
        return EntityIndicatorConverter.toDTO(
            eventIndicatorService.createEventIndicator(eventIndicatorDTO,
                tokenUtil.extractUserIdFromToken(bearerToken)));
    }

    @PutMapping("/{eventId}/{entityIndicatorId}")
    @PreAuthorize("hasAuthority('EDIT_EVENT_INDICATORS')")
    @EntityIndicatorEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateEventIndicator(
        @RequestBody EventIndicatorDTO eventIndicatorDTO,
        @PathVariable Integer entityIndicatorId) {
        eventIndicatorService.updateEventIndicator(entityIndicatorId, eventIndicatorDTO);
    }
}