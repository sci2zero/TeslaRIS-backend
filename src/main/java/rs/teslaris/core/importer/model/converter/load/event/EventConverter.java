package rs.teslaris.core.importer.model.converter.load.event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.model.oaipmh.event.Event;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;

@Component
@RequiredArgsConstructor
public class EventConverter implements RecordConverter<Event, ConferenceDTO> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final CountryService countryService;


    @Override
    public ConferenceDTO toDTO(Event record) {
        if (!record.getEventType().getValue().endsWith("Conference")) {
            return null;
        }

        var dto = new ConferenceDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        dto.setName(multilingualContentConverter.toDTO(record.getEventName()));
        dto.setPlace(multilingualContentConverter.toDTO(record.getPlace()));
        dto.setDescription(multilingualContentConverter.toDTO((String) record.getDescription()));

        var countryOptional = countryService.findCountryByName(record.getCountry());
        countryOptional.ifPresent(country -> dto.setCountryId(country.getId()));

        if (Objects.nonNull(record.getKeywords())) {
            var keywordBuilder = new StringBuilder();
            record.getKeywords().stream()
                .map(Object::toString)
                .forEach(keyword -> {
                    if (!keywordBuilder.isEmpty()) {
                        keywordBuilder.append(", ");
                    }
                    keywordBuilder.append(keyword);
                });
            dto.setKeywords(multilingualContentConverter.toDTO(keywordBuilder.toString()));
        } else {
            dto.setKeywords(new ArrayList<>());
        }

        var formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        try {
            dto.setDateFrom(
                LocalDate.ofInstant(formatter.parse(record.getStartDate()).toInstant(),
                    ZoneId.systemDefault()));
            dto.setDateTo(
                LocalDate.ofInstant(formatter.parse(record.getEndDate()).toInstant(),
                    ZoneId.systemDefault()));

            // In old CRIS, unknown exact date was modeled as like the event took place on 1.1.
            // This code translates that approach into ours (makes the event last a whole year)
            if (dto.getDateFrom().getDayOfMonth() == 1 &&
                dto.getDateFrom().getMonth().equals(Month.JANUARY) &&
                dto.getDateTo().getDayOfMonth() == 1 &&
                dto.getDateTo().getMonth().equals(Month.JANUARY)) {
                var endOfYearDate = LocalDate.of(dto.getDateTo().getYear(), 12, 31);
                dto.setDateTo(endOfYearDate);
            }
        } catch (ParseException e) {
            // pass
        }

        dto.setNameAbbreviation(new ArrayList<>());
        dto.setSerialEvent(false);
        dto.setContributions(new ArrayList<>());

        return dto;
    }
}
