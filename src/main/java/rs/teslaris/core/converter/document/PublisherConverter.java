package rs.teslaris.core.converter.document;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.model.document.Publisher;

public class PublisherConverter {

    public static PublisherDTO toDTO(Publisher publisher) {
        var dto = new PublisherDTO();
        dto.setId(publisher.getId());
        dto.setName(MultilingualContentConverter.getMultilingualContentDTO(publisher.getName()));
        dto.setPlace(MultilingualContentConverter.getMultilingualContentDTO(publisher.getPlace()));

        if (Objects.nonNull(publisher.getCountry())) {
            dto.setCountryId(publisher.getCountry().getId());
        }

        return dto;
    }
}
