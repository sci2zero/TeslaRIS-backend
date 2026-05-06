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
import rs.teslaris.core.annotation.OrgUnitEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.dto.identifier.OrganisationUnitIdentifierDTO;
import rs.teslaris.core.service.interfaces.identifier.OrganisationUnitIdentifierService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/organisation-unit-identifier")
@RequiredArgsConstructor
@Traceable
public class OrganisationUnitIdentifierController {

    private final OrganisationUnitIdentifierService organisationUnitIdentifierService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{organisationUnitId}")
    public Object getOrganisationUnitIdentifiers(HttpServletRequest request,
                                                 @PathVariable Integer organisationUnitId,
                                                 @RequestHeader(value = "Authorization", required = false)
                                                 String bearerToken,
                                                 @CookieValue(value = "jwt-security-fingerprint", required = false)
                                                 String fingerprintCookie) {
        return EntityIdentifierController.fetchIdentifiers(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> organisationUnitIdentifierService.getIdentifiersForOrganisationUnit(
                organisationUnitId,
                accessLevel)
        );
    }

    @PostMapping("/{organisationUnitId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_ORGANISATION_UNIT_IDENTIFIERS')")
    @OrgUnitEditCheck
    @Idempotent
    @Transactional
    public EntityIdentifierResponseDTO createOrganisationUnitIdentifier(
        @RequestBody OrganisationUnitIdentifierDTO organisationUnitIdentifierDTO,
        @RequestHeader("Authorization") String bearerToken) {
        return EntityIdentifierConverter.toDTO(
            organisationUnitIdentifierService.createOrganisationUnitIdentifier(
                organisationUnitIdentifierDTO,
                tokenUtil.extractUserIdFromToken(bearerToken)));
    }

    @PutMapping("/{organisationUnitId}/{entityIdentifierId}")
    @PreAuthorize("hasAuthority('EDIT_ORGANISATION_UNIT_IDENTIFIERS')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOrganisationUnitIdentifier(
        @RequestBody OrganisationUnitIdentifierDTO organisationUnitIdentifierDTO,
        @PathVariable Integer entityIdentifierId) {
        organisationUnitIdentifierService.updateOrganisationUnitIdentifier(entityIdentifierId,
            organisationUnitIdentifierDTO);
    }
}
