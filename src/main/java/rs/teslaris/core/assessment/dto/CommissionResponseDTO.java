package rs.teslaris.core.assessment.dto;

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

    Integer superCommissionId,

    List<MultilingualContentDTO> superCommissionDescription
) {
}
