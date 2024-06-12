package rs.teslaris.core.converter.person;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.person.PostalAddress;

public class PostalAddressConverter {

    public static PostalAddressDTO toDto(PostalAddress address) {
        var country = address.getCountry();
        return new PostalAddressDTO(country != null ? country.getId() : null,
            MultilingualContentConverter.getMultilingualContentDTO(
                address.getStreetAndNumber()),
            MultilingualContentConverter.getMultilingualContentDTO(
                address.getCity()));
    }
}
