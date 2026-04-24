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
import rs.teslaris.core.dto.identifier.PersonIdentifierDTO;
import rs.teslaris.core.service.interfaces.identifier.PersonIdentifierService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/person-identifier")
@RequiredArgsConstructor
@Traceable
public class PersonIdentifierController {

    private final PersonIdentifierService personIdentifierService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{personId}")
    public Object getPersonIdentifiers(HttpServletRequest request,
                                       @PathVariable Integer personId,
                                       @RequestHeader(value = "Authorization", required = false)
                                       String bearerToken,
                                       @CookieValue(value = "jwt-security-fingerprint", required = false)
                                       String fingerprintCookie) {
        return EntityIdentifierController.fetchIdentifiers(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> personIdentifierService.getIdentifiersForPerson(personId,
                accessLevel)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_PERSON_IDENTIFIERS')")
    @Idempotent
    @Transactional
    public EntityIdentifierResponseDTO createPersonIdentifier(
        @RequestBody PersonIdentifierDTO personIdentifierDTO,
        @RequestHeader("Authorization") String bearerToken) {
        return EntityIdentifierConverter.toDTO(
            personIdentifierService.createPersonIdentifier(personIdentifierDTO,
                tokenUtil.extractUserIdFromToken(bearerToken)));
    }

    @PutMapping("/{entityIdentifierId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_IDENTIFIERS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePersonIdentifier(
        @RequestBody PersonIdentifierDTO personIdentifierDTO,
        @PathVariable Integer entityIdentifierId) {
        personIdentifierService.updatePersonIdentifier(entityIdentifierId, personIdentifierDTO);
    }
}
