package rs.teslaris.core.assessment.controller;

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
import rs.teslaris.core.annotation.EntityIndicatorEditCheck;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.core.assessment.dto.DocumentIndicatorDTO;
import rs.teslaris.core.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.core.assessment.service.interfaces.DocumentIndicatorService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/document-indicator")
@RequiredArgsConstructor
public class DocumentIndicatorController {

    private final DocumentIndicatorService documentIndicatorService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{documentId}")
    public Object getDocumentIndicators(HttpServletRequest request,
                                        @PathVariable Integer documentId,
                                        @RequestHeader(value = "Authorization", required = false)
                                        String bearerToken,
                                        @CookieValue(value = "jwt-security-fingerprint", required = false)
                                        String fingerprintCookie) {
        return EntityIndicatorController.fetchIndicators(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> documentIndicatorService.getIndicatorsForDocument(documentId,
                accessLevel)
        );
    }

    @PostMapping("/{documentId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck
    @Idempotent
    public EntityIndicatorResponseDTO createDocumentIndicator(
        @RequestBody DocumentIndicatorDTO documentIndicatorDTO,
        @RequestHeader("Authorization") String bearerToken) {
        return EntityIndicatorConverter.toDTO(
            documentIndicatorService.createDocumentIndicator(documentIndicatorDTO,
                tokenUtil.extractUserIdFromToken(bearerToken)));
    }

    @PutMapping("/{documentId}/{entityIndicatorId}")
    @PreAuthorize("hasAuthority('EDIT_ENTITY_INDICATOR')")
    @PublicationEditCheck
    @EntityIndicatorEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDocumentIndicator(
        @RequestBody DocumentIndicatorDTO documentIndicatorDTO,
        @PathVariable Integer entityIndicatorId) {
        documentIndicatorService.updateDocumentIndicator(entityIndicatorId, documentIndicatorDTO);
    }
}
