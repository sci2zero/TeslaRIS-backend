package rs.teslaris.core.converter.document;

import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.model.document.Conference;

public class ConferenceConverter extends EventConverter {

    public static ConferenceDTO toDTO(Conference conference) {
        var dto = fillCommonFields(conference, new ConferenceDTO());

        dto.setNumber(conference.getNumber());
        dto.setFee(conference.getFee());
        dto.setConfId(conference.getConfId());
        dto.setOpenAlexId(conference.getOpenAlexId());

        return dto;
    }
}
