package rs.teslaris.thesislibrary.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.util.Pair;

public record InstitutionCountsReportDTO(
    List<MultilingualContentDTO> institutionName,
    Pair<Integer, Integer> counts
) {
}
