package rs.teslaris.thesislibrary.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record ThesisReportCountsDTO(
    Integer institutionId,
    List<MultilingualContentDTO> institutionName,
    Integer defendedCount,
    Integer putOnPublicReviewCount,
    Integer topicsAcceptedCount,
    Integer publiclyAvailableCount
) {
}
