package rs.teslaris.revisioner.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record DataQualityIssueOccurrenceDTO(

    List<String> actualValue,

    List<MultilingualContentDTO> message
) {
}
