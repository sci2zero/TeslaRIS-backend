package rs.teslaris.core.importer.model.converter.load.publication;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.model.oaipmh.publication.Publication;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@Component
@Slf4j
public class ProceedingsConverter extends DocumentConverter
    implements RecordConverter<Publication, ProceedingsDTO> {

    private final EventService eventService;

    private final LanguageTagService languageTagService;


    @Autowired
    public ProceedingsConverter(MultilingualContentConverter multilingualContentConverter,
                                PersonContributionConverter personContributionConverter,
                                EventService eventService, LanguageTagService languageTagService) {
        super(multilingualContentConverter, personContributionConverter);
        this.eventService = eventService;
        this.languageTagService = languageTagService;
    }

    @Override
    public ProceedingsDTO toDTO(Publication record) {
        var dto = new ProceedingsDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        setCommonFields(record, dto);

        dto.setEISBN(record.getIsbn());

        if (Objects.nonNull(record.getLanguage())) {
            var languageTagValue = record.getLanguage().trim().toUpperCase();
            if (languageTagValue.isEmpty()) {
                languageTagValue = LanguageAbbreviations.ENGLISH;
            }

            var languageTag = languageTagService.findLanguageTagByValue(languageTagValue);
            if (Objects.nonNull(languageTag.getId())) {
                dto.setLanguageTagIds(List.of(languageTag.getId()));
            } else {
                log.warn("No saved language with tag: " + languageTagValue);
                return null;
            }
        } else {
            dto.setLanguageTagIds(List.of());
        }

        if (Objects.nonNull(record.getOutputFrom().getEvent().getOldId()) &&
            !record.getOutputFrom().getEvent().getOldId().equals("null")) {
            var event = eventService.findEventByOldId(
                OAIPMHParseUtility.parseBISISID(record.getOutputFrom().getEvent().getOldId()));
            if (Objects.nonNull(event)) {
                dto.setEventId(event.getId());
            } else {
                log.warn("No saved event with id: " + record.getOutputFrom().getEvent().getOldId());
                return null;
            }

            if (dto.getTitle().isEmpty()) {
                dto.setTitle(
                    rs.teslaris.core.converter.commontypes.MultilingualContentConverter.getMultilingualContentDTO(
                        event.getName()));
            }
        } else {
            log.warn("Could not load, no event specified: " + record.getOldId());
            return null;
        }

        return dto;
    }
}
