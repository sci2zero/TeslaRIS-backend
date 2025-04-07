package rs.teslaris.thesislibrary.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.PromotionException;
import rs.teslaris.thesislibrary.dto.PromotionDTO;
import rs.teslaris.thesislibrary.model.Promotion;
import rs.teslaris.thesislibrary.repository.PromotionRepository;
import rs.teslaris.thesislibrary.service.interfaces.PromotionService;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl extends JPAServiceImpl<Promotion> implements PromotionService {

    private final PromotionRepository promotionRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    protected JpaRepository<Promotion, Integer> getEntityRepository() {
        return promotionRepository;
    }

    @Override
    public List<PromotionDTO> getNonFinishedPromotions() {
        return promotionRepository.getNonFinishedPromotions().stream().map(
            promotion -> new PromotionDTO(promotion.getId(), promotion.getPromotionDate(),
                promotion.getPromotionTime(),
                promotion.getPlaceOrVenue(), MultilingualContentConverter.getMultilingualContentDTO(
                promotion.getDescription()))).toList();
    }

    @Override
    public Promotion createPromotion(PromotionDTO promotionDTO) {
        var newPromotion = new Promotion();

        setCommonFields(newPromotion, promotionDTO);

        return save(newPromotion);
    }

    @Override
    public void updatePromotion(Integer promotionId, PromotionDTO promotionDTO) {
        var promotionToUpdate = findOne(promotionId);

        setCommonFields(promotionToUpdate, promotionDTO);

        save(promotionToUpdate);
    }

    private void setCommonFields(Promotion promotion, PromotionDTO promotionDTO) {
        promotion.setPromotionDate(promotionDTO.getPromotionDate());
        promotion.setPromotionTime(promotionDTO.getPromotionTime());
        promotion.setPlaceOrVenue(promotionDTO.getPlaceOrVenue());
        promotion.setDescription(
            multilingualContentService.getMultilingualContent(promotionDTO.getDescription()));
    }

    @Override
    public void deletePromotion(Integer promotionId) {
        if (promotionRepository.hasPromotableEntries(promotionId)) {
            throw new PromotionException("Promotion has entries.");
        }

        promotionRepository.delete(findOne(promotionId));
    }
}
