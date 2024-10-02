package rs.teslaris.core.converter.assessment;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.assessment.IndicatorDTO;
import rs.teslaris.core.model.assessment.Indicator;

public class IndicatorConverter {

    public static IndicatorDTO toDTO(Indicator indicator) {
        return new IndicatorDTO(
            indicator.getId(), indicator.getCode(),
            MultilingualContentConverter.getMultilingualContentDTO(indicator.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(indicator.getDescription()));
    }
}
