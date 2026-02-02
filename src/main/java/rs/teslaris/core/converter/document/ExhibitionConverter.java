package rs.teslaris.core.converter.document;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PersonContributionConverter;
import rs.teslaris.core.dto.document.ExhibitionDTO;
import rs.teslaris.core.model.document.Exhibition;

public class ExhibitionConverter {

    public static ExhibitionDTO toDTO(Exhibition exhibition) {
        var exhibitionDTO = new ExhibitionDTO();

        exhibitionDTO.setId(exhibition.getId());

        if (Objects.nonNull(exhibition.getOldIds())) {
            exhibition.getOldIds().stream().findFirst().ifPresent(exhibitionDTO::setOldId);
        }

        exhibitionDTO.setName(
            MultilingualContentConverter.getMultilingualContentDTO(exhibition.getName()));
        exhibitionDTO.setNameAbbreviation(MultilingualContentConverter.getMultilingualContentDTO(
            exhibition.getNameAbbreviation()));
        exhibitionDTO.setDescription(MultilingualContentConverter.getMultilingualContentDTO(
            exhibition.getDescription()));
        exhibitionDTO.setKeywords(MultilingualContentConverter.getMultilingualContentDTO(
            exhibition.getKeywords()));
        exhibitionDTO.setDateFrom(exhibition.getDateFrom());
        exhibitionDTO.setDateTo(exhibition.getDateTo());

        if (Objects.nonNull(exhibition.getCountry())) {
            exhibitionDTO.setCountryId(exhibition.getCountry().getId());
        }

        exhibitionDTO.setPlace(
            MultilingualContentConverter.getMultilingualContentDTO(exhibition.getPlace()));

        exhibitionDTO.setContributions(
            PersonContributionConverter.eventContributionToDTO(exhibition.getContributions()));

        exhibitionDTO.setNumber(exhibition.getNumber());
        exhibitionDTO.setFee(exhibition.getFee());
        exhibitionDTO.setSerialEvent(exhibition.getSerialEvent());

        exhibitionDTO.setUris(exhibition.getUris());

        return exhibitionDTO;
    }
}
