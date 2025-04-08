package rs.teslaris.thesislibrary.converter;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.thesislibrary.dto.PromotionDTO;
import rs.teslaris.thesislibrary.model.Promotion;

public class PromotionConverter {

    public static PromotionDTO toDTO(Promotion promotion) {
        return new PromotionDTO(promotion.getId(), promotion.getPromotionDate(),
            promotion.getPromotionTime(),
            promotion.getPlaceOrVenue(), MultilingualContentConverter.getMultilingualContentDTO(
            promotion.getDescription()));
    }
}
