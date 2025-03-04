package rs.teslaris.core.dto.commontypes;

import java.util.List;

public record BrandingInformationDTO(

    List<MultilingualContentDTO> title,
    List<MultilingualContentDTO> description
) {
}
