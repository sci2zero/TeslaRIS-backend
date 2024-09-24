package rs.teslaris.core.dto.assessment;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record AssessmentClassificationDTO(
    String formalDescriptionOfRule,
    String code,
    List<MultilingualContentDTO> title
) {
}
