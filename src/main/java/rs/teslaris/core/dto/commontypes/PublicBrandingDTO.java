package rs.teslaris.core.dto.commontypes;

import java.util.List;
import rs.teslaris.core.model.commontypes.BrandingTheme;

public record PublicBrandingDTO(

    List<MultilingualContentDTO> title,

    List<MultilingualContentDTO> description,

    String logoUrl,

    String backgroundUrl,

    BrandingTheme chromeTheme,

    BrandingTheme heroTheme
) {
}
