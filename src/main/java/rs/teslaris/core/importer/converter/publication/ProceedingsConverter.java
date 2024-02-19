package rs.teslaris.core.importer.converter.publication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.importer.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.publication.Publication;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@Component
@RequiredArgsConstructor
public class ProceedingsConverter implements RecordConverter<Publication, ProceedingsDTO> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final EventService eventService;

    private final LanguageTagService languageTagService;


    @Override
    public ProceedingsDTO toDTO(Publication record) {
        var dto = new ProceedingsDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getId()));

        dto.setTitle(multilingualContentConverter.toDTO(record.getTitle()));
        dto.setSubTitle(multilingualContentConverter.toDTO(record.getSubtitle()));
        dto.setDocumentDate(record.getPublicationDate().toString());

        dto.setEISBN(record.getIsbn());
        dto.setUris(new HashSet<>(record.getUrl()));

        var languageTagValue = record.getLanguage().trim().toUpperCase();
        if (languageTagValue.isEmpty()) {
            languageTagValue = LanguageAbbreviations.ENGLISH;
        }
        var languageTag = languageTagService.findLanguageTagByValue(languageTagValue);
        dto.setLanguageTagIds(List.of(languageTag.getId()));

        var event = eventService.findEventByOldId(
            OAIPMHParseUtility.parseBISISID(record.getOutputFrom().getEvent().getId()));
        if (Objects.nonNull(event)) {
            dto.setEventId(event.getId());
        } else {
            System.out.println(
                "No saved event with id: " + record.getOutputFrom().getEvent().getId());
            return null;
        }


        if (dto.getTitle().isEmpty()) {
            dto.setTitle(
                rs.teslaris.core.converter.commontypes.MultilingualContentConverter.getMultilingualContentDTO(
                    event.getName()));
        }

        dto.setContributions(new ArrayList<>());
        dto.setDescription(new ArrayList<>());
        dto.setKeywords(new ArrayList<>());

        return dto;
    }
}
