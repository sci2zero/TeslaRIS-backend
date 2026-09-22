package rs.teslaris.core.converter.person;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.person.PostalAddress;

public class PostalAddressConverter {

    public static PostalAddressDTO toDto(PostalAddress address) {
        if (Objects.isNull(address)) {
            return new PostalAddressDTO();
        }

        var country = address.getCountry();
        return new PostalAddressDTO(Objects.nonNull(country) ? country.getId() : null,
            MultilingualContentConverter.getMultilingualContentDTO(
                address.getStreetAndNumber()),
            MultilingualContentConverter.getMultilingualContentDTO(
                address.getCity()),
            MultilingualContentConverter.getMultilingualContentDTO(
                address.getState()),
            address.getPostalNumber());
    }
}
