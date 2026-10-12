package rs.teslaris.revisioner.dto;

import java.util.List;
import java.util.Map;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record DataQualityProfileDTO(
    String profileName,

    String version,

    List<MultilingualContentDTO> name,

    Map<String, DataQualityRemarkDTO> dataQualityRemarks
) {
}
