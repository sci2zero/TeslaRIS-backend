package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.document.DocumentClaimingService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/document-claim")
@RequiredArgsConstructor
@Traceable
public class DocumentClaimingController {

    private final DocumentClaimingService documentClaimingService;

    private final JwtUtil tokenUtil;


    @GetMapping
    @PreAuthorize("hasAuthority('CLAIM_DOCUMENT')")
    public Page<DocumentPublicationIndex> findPotentialDocumentClaimsForPerson(
        @RequestHeader("Authorization") String bearerToken, Pageable pageable) {
        return documentClaimingService.findPotentialClaimsForPerson(
            tokenUtil.extractUserIdFromToken(bearerToken), pageable);
    }

    @PatchMapping("/decline/{documentId}")
    @PreAuthorize("hasAuthority('CLAIM_DOCUMENT')")
    public void declinePublicationClaim(@RequestHeader("Authorization") String bearerToken,
                                        @PathVariable Integer documentId) {
        documentClaimingService.declineDocumentClaim(tokenUtil.extractUserIdFromToken(bearerToken),
            documentId);
    }

    @PatchMapping("/{documentId}")
    @PreAuthorize("hasAuthority('CLAIM_DOCUMENT')")
    public void claimPublication(@RequestHeader("Authorization") String bearerToken,
                                 @PathVariable Integer documentId) {
        documentClaimingService.claimDocument(tokenUtil.extractUserIdFromToken(bearerToken),
            documentId);
    }
}
