package rs.teslaris.core.controller.identifier;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.dto.identifier.EventIdentifierDTO;
import rs.teslaris.core.service.interfaces.identifier.EventIdentifierService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/event-identifier")
@RequiredArgsConstructor
@Traceable
public class EventIdentifierController {

    private final EventIdentifierService eventIdentifierService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{eventId}")
    public Object getEventIdentifiers(HttpServletRequest request,
                                      @PathVariable Integer eventId,
                                      @RequestHeader(value = "Authorization", required = false)
                                      String bearerToken,
                                      @CookieValue(value = "jwt-security-fingerprint", required = false)
                                      String fingerprintCookie) {
        return EntityIdentifierController.fetchIdentifiers(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> eventIdentifierService.getIdentifiersForEvent(eventId,
                accessLevel)
        );
    }

    @PostMapping("/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_EVENT_IDENTIFIERS')")
    @Idempotent
    @Transactional
    public EntityIdentifierResponseDTO createEventIdentifier(
        @RequestBody EventIdentifierDTO eventIdentifierDTO,
        @RequestHeader("Authorization") String bearerToken) {
        return EntityIdentifierConverter.toDTO(
            eventIdentifierService.createEventIdentifier(eventIdentifierDTO,
                tokenUtil.extractUserIdFromToken(bearerToken)));
    }

    @PutMapping("/{eventId}/{entityIdentifierId}")
    @PreAuthorize("hasAuthority('EDIT_EVENT_IDENTIFIERS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateEventIdentifier(
        @RequestBody EventIdentifierDTO eventIdentifierDTO,
        @PathVariable Integer entityIdentifierId) {
        eventIdentifierService.updateEventIdentifier(entityIdentifierId, eventIdentifierDTO);
    }
}
