package rs.teslaris.revisioner.dto;

import java.time.LocalDate;

public record DataQualityAssessmentSimpleDTO(
    String profileName,
    String profileVersion,
    Double dataQualityScore,
    boolean publicationCandidate,
    LocalDate assessmentDate
) {
}
