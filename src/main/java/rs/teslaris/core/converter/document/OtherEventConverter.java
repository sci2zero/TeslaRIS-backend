package rs.teslaris.core.converter.document;

import rs.teslaris.core.dto.document.OtherEventDTO;
import rs.teslaris.core.model.document.OtherEvent;

public class OtherEventConverter extends EventConverter {

    public static OtherEventDTO toDTO(OtherEvent event) {
        var dto = fillCommonFields(event, new OtherEventDTO());

        dto.setType(event.getType());

        return dto;
    }
}
