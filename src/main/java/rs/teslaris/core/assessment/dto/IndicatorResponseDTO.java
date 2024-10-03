package rs.teslaris.core.assessment.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record IndicatorResponseDTO(

    Integer id,

    String code,

    List<MultilingualContentDTO> title,

    List<MultilingualContentDTO> description
) {
}
