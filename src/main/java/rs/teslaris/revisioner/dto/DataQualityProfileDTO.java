package rs.teslaris.revisioner.dto;

import java.util.Map;

public record DataQualityProfileDTO(
    String version,

    Map<String, DataQualityRemarkDTO> dataQualityRemarks
) {
}
