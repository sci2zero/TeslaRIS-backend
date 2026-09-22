package rs.teslaris.revisioner.dto;

import java.time.Instant;
import java.util.List;
import rs.teslaris.core.util.restoration.DegradedReference;

public record RevisionDTO(
    Instant timestamp,
    Integer majorVersion,
    Integer minorVersion,
    String versionNote,
    String createdBy,
    List<DataQualityAssessmentSimpleDTO> assessments,
    List<DegradedReference> restorationWarnings
) {
}
