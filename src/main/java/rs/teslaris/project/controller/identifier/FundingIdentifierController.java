package rs.teslaris.project.controller.identifier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import rs.teslaris.core.controller.identifier.EntityIdentifierController;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.project.dto.identifier.FundingIdentifierDTO;
import rs.teslaris.project.service.interfaces.identifier.FundingIdentifierService;

@RestController
@RequestMapping("/api/funding-identifier")
@RequiredArgsConstructor
@Traceable
public class FundingIdentifierController {

    private final FundingIdentifierService fundingIdentifierService;


    @GetMapping("/{fundingId}")
    public Object getFundingIdentifiers(HttpServletRequest request,
                                    @PathVariable Integer fundingId,
                                    @RequestHeader(value = "Authorization", required = false)
                                    String bearerToken,
                                    @CookieValue(value = "jwt-security-fingerprint", required = false)
                                    String fingerprintCookie) {
        return EntityIdentifierController.fetchIdentifiers(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> fundingIdentifierService.getIdentifiersForFunding(fundingId, accessLevel)
        );
    }

    @PostMapping("/{fundingId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_FUNDING_IDENTIFIERS')")
    @Idempotent
    @Transactional
    public EntityIdentifierResponseDTO createFundingIdentifier(
        @PathVariable Integer fundingId,
        @RequestBody @Valid FundingIdentifierDTO fundingIdentifierDTO) {
        // The path ID is the one the permission check saw, so it wins over the body.
        fundingIdentifierDTO.setFundingId(fundingId);

        return EntityIdentifierConverter.toDTO(
            fundingIdentifierService.createFundingIdentifier(fundingIdentifierDTO));
    }

    @PutMapping("/{fundingId}/{entityIdentifierId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_IDENTIFIERS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFundingIdentifier(
        @PathVariable Integer fundingId,
        @PathVariable Integer entityIdentifierId,
        @RequestBody @Valid FundingIdentifierDTO fundingIdentifierDTO) {
        fundingIdentifierDTO.setFundingId(fundingId);

        fundingIdentifierService.updateFundingIdentifier(entityIdentifierId, fundingIdentifierDTO);
    }
}
