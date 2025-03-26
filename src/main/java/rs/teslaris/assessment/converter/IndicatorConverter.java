package rs.teslaris.assessment.converter;

import rs.teslaris.assessment.dto.IndicatorResponseDTO;
import rs.teslaris.assessment.model.Indicator;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;

public class IndicatorConverter {

    public static IndicatorResponseDTO toDTO(Indicator indicator) {
        return new IndicatorResponseDTO(
            indicator.getId(), indicator.getCode(),
            MultilingualContentConverter.getMultilingualContentDTO(indicator.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(indicator.getDescription()),
            indicator.getApplicableTypes().stream().toList(), indicator.getContentType());
    }
}
