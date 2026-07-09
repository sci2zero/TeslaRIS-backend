package rs.teslaris.revisioner.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record QualityReportResponseDTO(
    String profileName,
    List<MultilingualContentDTO> report
) {

}
