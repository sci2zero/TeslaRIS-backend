package rs.teslaris.revisioner.dto;

import java.time.LocalDate;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;

public record QualityReportResponseDTO(
    String profileName,
    Double qualityScore,
    int issueCount,
    LocalDate assessmentDate,
    boolean publicationCandidate,
    List<Pair<IssueSeverity, List<MultilingualContentDTO>>> report
) {
}
