package rs.teslaris.core.controller.person;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.core.dto.person.PrizeResponseDTO;
import rs.teslaris.core.indexmodel.PrizeIndex;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@Validated
@RestController
@RequestMapping("/api/prize")
@RequiredArgsConstructor
@Traceable
public class PrizeController {

    private final PrizeService prizeService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/simple-search")
    public Page<PrizeIndex> simpleSearch(
        @RequestParam(value = "tokens", required = false) List<String> tokens,
        @RequestParam(required = false) Integer institutionId,
        @RequestParam(required = false) Integer personId,
        @RequestParam(value = "unclassified", defaultValue = "false") Boolean unclassified,
        @RequestHeader(value = "Authorization", defaultValue = "") String bearerToken,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);

        var isCommission = !bearerToken.isEmpty() &&
            tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.COMMISSION.name());

        return prizeService.searchPrizes(tokens, pageable, personId, institutionId,
            (isCommission && unclassified) ?
                userService.getUserCommissionId(tokenUtil.extractUserIdFromToken(bearerToken)) :
                null);
    }

    @PostMapping("/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    @ResponseStatus(HttpStatus.CREATED)
    public PrizeResponseDTO addPrize(
        @RequestBody PrizeDTO prize, @PathVariable Integer personId) {
        return prizeService.addPrize(personId, prize);
    }

    @PutMapping("/{personId}/{prizeId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public PrizeResponseDTO updatePrize(
        @RequestBody PrizeDTO prize,
        @PathVariable Integer prizeId) {
        return prizeService.updatePrize(prizeId, prize);
    }

    @DeleteMapping("/{personId}/{prizeId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePrize(@PathVariable Integer prizeId,
                            @PathVariable Integer personId) {
        prizeService.deletePrize(prizeId, personId);
    }

    @PatchMapping(value = "/{personId}/{prizeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    public DocumentFileResponseDTO addProof(@ModelAttribute @Valid DocumentFileDTO proof,
                                            @PathVariable Integer prizeId) {
        return prizeService.addProof(prizeId, proof);
    }

    @PatchMapping(value = "/{personId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    public DocumentFileResponseDTO updatePrizeProof(
        @ModelAttribute @Valid DocumentFileDTO proof) {
        return prizeService.updateProof(proof);
    }

    @DeleteMapping("/{personId}/{prizeId}/{proofId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public void deletePrizeProof(@PathVariable Integer prizeId,
                                 @PathVariable Integer proofId) {
        prizeService.deleteProof(proofId, prizeId);
    }
}
