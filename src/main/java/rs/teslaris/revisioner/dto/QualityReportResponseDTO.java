package rs.teslaris.revisioner.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;

public record QualityReportResponseDTO(
    String profileName,
    List<Pair<IssueSeverity, List<MultilingualContentDTO>>> report
) {
}
