package rs.teslaris.core.assessment.dto;

import java.time.LocalDateTime;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record EntityAssessmentClassificationResponseDTO(
    List<MultilingualContentDTO> classificationTitle,

    LocalDateTime timestamp
) {
}
