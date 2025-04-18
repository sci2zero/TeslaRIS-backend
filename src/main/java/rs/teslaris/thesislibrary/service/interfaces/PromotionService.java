package rs.teslaris.thesislibrary.service.interfaces;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.thesislibrary.dto.PromotionDTO;
import rs.teslaris.thesislibrary.model.Promotion;

@Service
public interface PromotionService extends JPAService<Promotion> {

    Page<PromotionDTO> getAllPromotions(Integer institutionId, Pageable pageable);

    List<PromotionDTO> getNonFinishedPromotions(Integer institutionId);

    Promotion createPromotion(PromotionDTO promotionDTO);

    Promotion migratePromotion(PromotionDTO promotionDTO);

    void updatePromotion(Integer promotionId, PromotionDTO promotionDTO);

    void deletePromotion(Integer promotionId);
}
