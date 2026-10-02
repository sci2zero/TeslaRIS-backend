package rs.teslaris.core.dto.commontypes;

import java.util.List;

public record PublicBrandingDTO(

    List<MultilingualContentDTO> title,

    List<MultilingualContentDTO> description,

    String logoUrl,

    String backgroundUrl
) {
}
