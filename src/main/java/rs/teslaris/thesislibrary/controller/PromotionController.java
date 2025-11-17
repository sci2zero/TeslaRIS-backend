package rs.teslaris.thesislibrary.controller;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.thesislibrary.annotation.PromotionEditAndUsageCheck;
import rs.teslaris.thesislibrary.dto.PromotionDTO;
import rs.teslaris.thesislibrary.service.interfaces.PromotionService;

@RestController
@RequestMapping("/api/promotion")
@RequiredArgsConstructor
@Traceable
public class PromotionController {

    private final PromotionService promotionService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGE_PROMOTIONS', 'READ_PROMOTIONS')")
    public Page<PromotionDTO> getAllPromotions(@RequestHeader("Authorization") String bearerToken,
                                               Pageable pageable) {
        return promotionService.getAllPromotions(getBelongingInstitution(bearerToken), pageable);
    }

    @GetMapping("/non-finished")
    @PreAuthorize("hasAnyAuthority('MANAGE_PROMOTIONS', 'READ_PROMOTIONS')")
    public List<PromotionDTO> getNonFinishedPromotionList(
        @RequestHeader("Authorization") String bearerToken) {
        return promotionService.getNonFinishedPromotions(getBelongingInstitution(bearerToken));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public PromotionDTO createPromotion(@RequestBody @Valid PromotionDTO promotionDTO,
                                        @RequestHeader("Authorization") String bearerToken) {
        handleInstitutionSetting(promotionDTO, bearerToken);

        var newPromotion = promotionService.createPromotion(promotionDTO);
        promotionDTO.setId(newPromotion.getId());

        return promotionDTO;
    }

    @PostMapping("/migrate")
    @PreAuthorize("hasAuthority('PERFORM_MIGRATION')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public PromotionDTO migratePromotion(@RequestBody PromotionDTO promotionDTO,
                                         @RequestHeader("Authorization") String bearerToken) {
        handleInstitutionSetting(promotionDTO, bearerToken);

        var newPromotion = promotionService.migratePromotion(promotionDTO);
        promotionDTO.setId(newPromotion.getId());

        return promotionDTO;
    }

    @PutMapping("/{promotionId}")
    @PromotionEditAndUsageCheck
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePromotion(@PathVariable Integer promotionId,
                                @RequestBody @Valid PromotionDTO promotionDTO,
                                @RequestHeader("Authorization") String bearerToken) {
        handleInstitutionSetting(promotionDTO, bearerToken);
        promotionService.updatePromotion(promotionId, promotionDTO);
    }

    @DeleteMapping("/{promotionId}")
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    @PromotionEditAndUsageCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePromotion(@PathVariable Integer promotionId) {
        promotionService.deletePromotion(promotionId);
    }

    private void handleInstitutionSetting(PromotionDTO promotionDTO, String bearerToken) {
        if (tokenUtil.extractUserRoleFromToken(bearerToken)
            .equals(UserRole.PROMOTION_REGISTRY_ADMINISTRATOR.toString())) {
            promotionDTO.setInstitutionId(userService.getUserOrganisationUnitId(
                tokenUtil.extractUserIdFromToken(bearerToken)));
        }
    }

    @Nullable
    private Integer getBelongingInstitution(String bearerToken) {
        Integer institutionId = null;
        if (!tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.ADMIN.toString())) {
            institutionId = userService.getUserOrganisationUnitId(
                tokenUtil.extractUserIdFromToken(bearerToken));
        }

        return institutionId;
    }
}
