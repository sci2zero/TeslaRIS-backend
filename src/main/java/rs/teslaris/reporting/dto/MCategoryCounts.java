package rs.teslaris.reporting.dto;

import java.util.List;
import java.util.Map;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record MCategoryCounts(
    List<MultilingualContentDTO> commissionName,
    Map<String, Long> countsByCategory
) {
}
