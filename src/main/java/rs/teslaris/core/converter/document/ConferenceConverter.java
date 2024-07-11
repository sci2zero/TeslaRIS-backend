package rs.teslaris.core.converter.document;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PersonContributionConverter;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.model.document.Conference;

public class ConferenceConverter {

    public static ConferenceDTO toDTO(Conference conference) {
        var conferenceDTO = new ConferenceDTO();

        conferenceDTO.setId(conference.getId());
        conferenceDTO.setName(
            MultilingualContentConverter.getMultilingualContentDTO(conference.getName()));
        conferenceDTO.setNameAbbreviation(MultilingualContentConverter.getMultilingualContentDTO(
            conference.getNameAbbreviation()));
        conferenceDTO.setDescription(MultilingualContentConverter.getMultilingualContentDTO(
            conference.getDescription()));
        conferenceDTO.setKeywords(MultilingualContentConverter.getMultilingualContentDTO(
            conference.getKeywords()));
        conferenceDTO.setDateFrom(conference.getDateFrom());
        conferenceDTO.setDateTo(conference.getDateTo());
        conferenceDTO.setState(
            MultilingualContentConverter.getMultilingualContentDTO(conference.getState()));
        conferenceDTO.setPlace(
            MultilingualContentConverter.getMultilingualContentDTO(conference.getPlace()));

        conferenceDTO.setContributions(
            PersonContributionConverter.eventContributionToDTO(conference.getContributions()));

        conferenceDTO.setNumber(conference.getNumber());
        conferenceDTO.setFee(conference.getFee());
        conferenceDTO.setSerialEvent(conference.getSerialEvent());
        conferenceDTO.setConfId(conference.getConfId());

        return conferenceDTO;
    }
}
