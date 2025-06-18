package rs.teslaris.assessment.dto.indicator;

import java.util.List;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
import rs.teslaris.assessment.model.indicator.IndicatorContentType;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record IndicatorResponseDTO(

    Integer id,

    String code,

    List<MultilingualContentDTO> title,

    List<MultilingualContentDTO> description,

    List<ApplicableEntityType> applicableEntityTypes,

    IndicatorContentType contentType
) {
}
