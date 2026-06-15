package rs.teslaris.core.converter.document;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PersonContributionConverter;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.model.document.Event;

public class EventConverter {

    protected static <T extends EventDTO> T fillCommonFields(Event event, T dto) {
        dto.setId(event.getId());
        dto.setEventType(event.getEventType());

        if (Objects.nonNull(event.getOldIds())) {
            event.getOldIds().stream().findFirst().ifPresent(dto::setOldId);
        }

        dto.setName(
            MultilingualContentConverter.getMultilingualContentDTO(event.getName()));

        dto.setNameAbbreviation(
            MultilingualContentConverter.getMultilingualContentDTO(
                event.getNameAbbreviation()));

        dto.setDescription(
            MultilingualContentConverter.getMultilingualContentDTO(
                event.getDescription()));

        dto.setDisplayOrganizer(
            MultilingualContentConverter.getMultilingualContentDTO(
                event.getDisplayOrganizer()));

        dto.setKeywords(
            MultilingualContentConverter.getMultilingualContentDTO(
                event.getKeywords()));

        dto.setDateFrom(event.getDateFrom());
        dto.setDateTo(event.getDateTo());

        if (Objects.nonNull(event.getCountry())) {
            dto.setCountryId(event.getCountry().getId());
        }

        dto.setPlace(
            MultilingualContentConverter.getMultilingualContentDTO(event.getPlace()));

        dto.setContributions(
            PersonContributionConverter.eventContributionToDTO(event.getContributions()));

        dto.setSerialEvent(event.getSerialEvent());

        dto.setUris(event.getUris());

        return dto;
    }
}
