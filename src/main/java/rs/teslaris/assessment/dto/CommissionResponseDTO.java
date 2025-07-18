package rs.teslaris.assessment.dto;

import java.time.LocalDate;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record CommissionResponseDTO(

    Integer id,

    List<MultilingualContentDTO> description,

    List<String> sources,

    LocalDate assessmentDateFrom,

    LocalDate assessmentDateTo,

    String formalDescriptionOfRule,

    List<String> recognisedResearchAreas,

    Boolean isDefault
) {
}
