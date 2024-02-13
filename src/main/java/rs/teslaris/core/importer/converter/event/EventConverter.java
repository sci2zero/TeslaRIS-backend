package rs.teslaris.core.importer.converter.event;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.importer.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.event.Event;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;

@Component
@RequiredArgsConstructor
public class EventConverter implements RecordConverter<Event, ConferenceDTO> {

    private final MultilingualContentConverter multilingualContentConverter;


    @Override
    public ConferenceDTO toDTO(Event record) {
        if (!record.getEventType().getValue().endsWith("Conference")) {
            return null;
        }

        var dto = new ConferenceDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getId()));

        dto.setName(multilingualContentConverter.toDTO(record.getEventName()));
        dto.setPlace(multilingualContentConverter.toDTO(record.getPlace()));
        dto.setState(multilingualContentConverter.toDTO(record.getCountry()));
        dto.setDescription(multilingualContentConverter.toDTO(record.getDescription()));
        dto.setKeywords(
            multilingualContentConverter.toDTO(String.join(", ",
                Objects.requireNonNullElse(record.getKeywords(), new ArrayList<>()))));

        dto.setDateFrom(
            record.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        dto.setDateTo(record.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        dto.setNameAbbreviation(new ArrayList<>());
        dto.setSerialEvent(false);
        dto.setContributions(new ArrayList<>());

        return dto;
    }
}
