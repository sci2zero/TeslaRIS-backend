package rs.teslaris.core.assessment.dto;

import java.time.LocalDateTime;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record EntityAssessmentClassificationResponseDTO(
    Integer id,
    List<MultilingualContentDTO> classificationTitle,

    List<MultilingualContentDTO> commissionDescription,

    String categoryIdentifier,

    Integer year,

    LocalDateTime timestamp
) {
}
