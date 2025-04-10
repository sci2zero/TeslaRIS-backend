package rs.teslaris.thesislibrary.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.thesislibrary.dto.PromotionDTO;
import rs.teslaris.thesislibrary.service.interfaces.PromotionService;

@RestController
@RequestMapping("/api/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;


    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    public Page<PromotionDTO> getAllPromotions(Pageable pageable) {
        return promotionService.getAllPromotions(pageable);
    }

    @GetMapping("/non-finished")
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    public List<PromotionDTO> getNonFinishedPromotionList() {
        return promotionService.getNonFinishedPromotions();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public PromotionDTO createPromotion(@RequestBody @Valid PromotionDTO promotionDTO) {
        var newPromotion = promotionService.createPromotion(promotionDTO);
        promotionDTO.setId(newPromotion.getId());

        return promotionDTO;
    }

    @PutMapping("/{promotionId}")
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePromotion(@PathVariable Integer promotionId,
                                @RequestBody @Valid PromotionDTO promotionDTO) {
        promotionService.updatePromotion(promotionId, promotionDTO);
    }

    @DeleteMapping("/{promotionId}")
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePromotion(@PathVariable Integer promotionId) {
        promotionService.deletePromotion(promotionId);
    }
}
