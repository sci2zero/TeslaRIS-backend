package rs.teslaris.core.assessment.converter;

import rs.teslaris.core.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.core.assessment.model.EntityIndicator;

public class EntityIndicatorConverter {

    public static EntityIndicatorResponseDTO toDTO(EntityIndicator entityIndicator) {
        return new EntityIndicatorResponseDTO(entityIndicator.getNumericValue(),
            entityIndicator.getBooleanValue(),
            entityIndicator.getTextualValue(), entityIndicator.getFromDate(),
            entityIndicator.getToDate(), entityIndicator.getUrls().stream().toList(),
            IndicatorConverter.toDTO(entityIndicator.getIndicator()));
    }
}
