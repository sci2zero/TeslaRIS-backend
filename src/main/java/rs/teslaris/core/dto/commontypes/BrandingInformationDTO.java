package rs.teslaris.core.dto.commontypes;

import java.util.List;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.commontypes.BrandingTheme;

public record BrandingInformationDTO(

    List<MultilingualContentDTO> title,
    List<MultilingualContentDTO> description,
    GeoLocationDTO location,
    PostalAddressDTO postalAddress,
    String phoneNumber,
    BrandingTheme chromeTheme,
    BrandingTheme heroTheme
) {
}
