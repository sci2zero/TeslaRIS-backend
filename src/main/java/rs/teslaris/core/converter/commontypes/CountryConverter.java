package rs.teslaris.core.converter.commontypes;

import rs.teslaris.core.dto.commontypes.CountryDTO;
import rs.teslaris.core.model.commontypes.Country;

public class CountryConverter {

    public static CountryDTO toDTO(Country country) {
        var dto = new CountryDTO();
        dto.setId(country.getId());
        dto.setCode(country.getCode());
        dto.setName(MultilingualContentConverter.getMultilingualContentDTO(country.getName()));

        return dto;
    }
}
