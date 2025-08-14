package rs.teslaris.core.dto.institution;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record InstitutionDefaultSubmissionContentDTO(
    List<MultilingualContentDTO> typeOfTitle,
    List<MultilingualContentDTO> placeOfKeep
) {
}
