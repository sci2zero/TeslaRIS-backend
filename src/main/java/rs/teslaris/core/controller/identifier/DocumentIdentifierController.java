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
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.DocumentIdentifierDTO;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.service.interfaces.identifier.DocumentIdentifierService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/document-identifier")
@RequiredArgsConstructor
@Traceable
public class DocumentIdentifierController {

    private final DocumentIdentifierService documentIdentifierService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{documentId}")
    public Object getDocumentIdentifiers(HttpServletRequest request,
                                         @PathVariable Integer documentId,
                                         @RequestHeader(value = "Authorization", required = false)
                                         String bearerToken,
                                         @CookieValue(value = "jwt-security-fingerprint", required = false)
                                         String fingerprintCookie) {
        return EntityIdentifierController.fetchIdentifiers(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> documentIdentifierService.getIdentifiersForDocument(documentId,
                accessLevel)
        );
    }

    @PostMapping("/{documentId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_DOCUMENT_IDENTIFIERS')")
    @PublicationEditCheck
    @Idempotent
    @Transactional
    public EntityIdentifierResponseDTO createDocumentIdentifier(
        @RequestBody DocumentIdentifierDTO documentIdentifierDTO,
        @RequestHeader("Authorization") String bearerToken) {
        return EntityIdentifierConverter.toDTO(
            documentIdentifierService.createDocumentIdentifier(documentIdentifierDTO,
                tokenUtil.extractUserIdFromToken(bearerToken)));
    }

    @PutMapping("/{documentId}/{entityIdentifierId}")
    @PreAuthorize("hasAuthority('EDIT_DOCUMENT_IDENTIFIERS')")
    @PublicationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDocumentIdentifier(
        @RequestBody DocumentIdentifierDTO documentIdentifierDTO,
        @PathVariable Integer entityIdentifierId) {
        documentIdentifierService.updateDocumentIdentifier(entityIdentifierId,
            documentIdentifierDTO);
    }
}
