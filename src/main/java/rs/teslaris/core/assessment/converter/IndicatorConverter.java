package rs.teslaris.core.assessment.converter;

import rs.teslaris.core.assessment.dto.IndicatorResponseDTO;
import rs.teslaris.core.assessment.model.Indicator;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;

public class IndicatorConverter {

    public static IndicatorResponseDTO toDTO(Indicator indicator) {
        return new IndicatorResponseDTO(
            indicator.getId(), indicator.getCode(),
            MultilingualContentConverter.getMultilingualContentDTO(indicator.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(indicator.getDescription()));
    }
}
