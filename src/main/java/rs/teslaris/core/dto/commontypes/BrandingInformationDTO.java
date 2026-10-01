package rs.teslaris.core.dto.commontypes;

import java.util.List;
import rs.teslaris.core.dto.person.PostalAddressDTO;

public record BrandingInformationDTO(

    List<MultilingualContentDTO> title,
    List<MultilingualContentDTO> description,
    GeoLocationDTO location,
    PostalAddressDTO postalAddress,
    String phoneNumber
) {
}
