package rs.teslaris.importer.model.converter.load.publication;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@RequiredArgsConstructor
public class JournalConverter implements RecordConverter<Publication, JournalDTO> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final LanguageTagService languageTagService;


    @Override
    public JournalDTO toDTO(Publication record) {
        var dto = new JournalDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        dto.setTitle(multilingualContentConverter.toDTO(record.getTitle()));
        dto.setEissn(record.getIssn());

        dto.setContributions(new ArrayList<>());
        if (Objects.nonNull(record.getAcronym()) && !record.getAcronym().isEmpty()) {
            dto.setNameAbbreviation(multilingualContentConverter.toDTO(record.getAcronym()));
        } else {
            dto.setNameAbbreviation(new ArrayList<>());
        }

        dto.setSubtitle(new ArrayList<>());

        var languageTagValue = record.getLanguage().trim().toUpperCase();
        if (languageTagValue.isEmpty()) {
            languageTagValue = LanguageAbbreviations.ENGLISH;
        }

        if (languageTagValue.equals("GE")) {
            languageTagValue = LanguageAbbreviations.GERMAN;
        }

        var languageTag = languageTagService.findLanguageTagByValue(languageTagValue);
        if (Objects.nonNull(languageTag.getId())) {
            dto.setLanguageTagIds(List.of(languageTag.getId()));
        } else {
            dto.setLanguageTagIds(new ArrayList<>());
        }

        return dto;
    }
}
