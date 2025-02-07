package rs.teslaris.core.assessment.dto;

import java.time.LocalDateTime;
import java.util.List;
import rs.teslaris.core.assessment.model.ApplicableEntityType;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record EntityAssessmentClassificationResponseDTO(
    Integer id,
    List<MultilingualContentDTO> classificationTitle,

    Integer classificationId,

    List<MultilingualContentDTO> commissionDescription,

    Integer commissionId,

    String categoryIdentifier,

    Integer year,

    LocalDateTime timestamp,

    List<ApplicableEntityType> applicableEntityTypes,
    Boolean manual
) {
}
