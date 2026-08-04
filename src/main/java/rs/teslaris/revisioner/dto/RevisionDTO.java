package rs.teslaris.revisioner.dto;

import java.time.Instant;
import java.util.List;

public record RevisionDTO(
    Instant timestamp,
    Integer majorVersion,
    Integer minorVersion,
    String versionNote,
    String createdBy,
    List<DataQualityAssessmentSimpleDTO> assessments
) {
}
