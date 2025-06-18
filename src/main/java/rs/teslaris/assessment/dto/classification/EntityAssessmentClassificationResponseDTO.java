package rs.teslaris.assessment.dto.classification;

import java.time.LocalDateTime;
import java.util.List;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
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

    Boolean manual,

    List<MultilingualContentDTO> assessmentReason
) {
}
