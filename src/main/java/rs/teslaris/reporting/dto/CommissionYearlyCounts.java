package rs.teslaris.reporting.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record CommissionYearlyCounts(
    List<MultilingualContentDTO> commissionName,
    List<YearlyCounts> yearlyCounts
) {
}
