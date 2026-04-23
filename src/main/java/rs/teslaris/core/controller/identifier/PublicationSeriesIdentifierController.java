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
import rs.teslaris.core.dto.identifier.PublicationSeriesIdentifierDTO;
import rs.teslaris.core.service.interfaces.identifier.PublicationSeriesIdentifierService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/publication-series-identifier")
@RequiredArgsConstructor
@Traceable
public class PublicationSeriesIdentifierController {

    private final PublicationSeriesIdentifierService publicationSeriesIdentifierService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{publicationSeriesId}")
    public Object getPublicationSeriesIdentifiers(HttpServletRequest request,
                                                  @PathVariable Integer publicationSeriesId,
                                                  @RequestHeader(value = "Authorization", required = false)
                                                  String bearerToken,
                                                  @CookieValue(value = "jwt-security-fingerprint", required = false)
                                                  String fingerprintCookie) {
        return EntityIdentifierController.fetchIdentifiers(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> publicationSeriesIdentifierService.getIdentifiersForPublicationSeries(
                publicationSeriesId,
                accessLevel)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES_IDENTIFIERS')")
    @Idempotent
    @Transactional
    public EntityIdentifierResponseDTO createPublicationSeriesIdentifier(
        @RequestBody PublicationSeriesIdentifierDTO publicationSeriesIdentifierDTO,
        @RequestHeader("Authorization") String bearerToken) {
        return EntityIdentifierConverter.toDTO(
            publicationSeriesIdentifierService.createPublicationSeriesIdentifier(
                publicationSeriesIdentifierDTO,
                tokenUtil.extractUserIdFromToken(bearerToken)));
    }

    @PutMapping("/{entityIdentifierId}")
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES_IDENTIFIERS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePublicationSeriesIdentifier(
        @RequestBody PublicationSeriesIdentifierDTO publicationSeriesIdentifierDTO,
        @PathVariable Integer entityIdentifierId) {
        publicationSeriesIdentifierService.updatePublicationSeriesIdentifier(entityIdentifierId,
            publicationSeriesIdentifierDTO);
    }
}
