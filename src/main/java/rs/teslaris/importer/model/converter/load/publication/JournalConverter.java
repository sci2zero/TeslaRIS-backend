package rs.teslaris.importer.model.converter.load.publication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@RequiredArgsConstructor
public class JournalConverter implements RecordConverter<Publication, JournalDTO> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final LanguageService languageService;


    @Override
    public JournalDTO toDTO(Publication record) {
        var dto = new JournalDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        dto.setTitle(multilingualContentConverter.toDTO(record.getTitle()));
        if (Objects.nonNull(record.getIssn()) && !record.getIssn().isEmpty()) {
            dto.setEissn(record.getIssn().getFirst());

            if (record.getIssn().size() > 1) {
                dto.setPrintISSN(record.getIssn().getLast());
            }
        }

        dto.setContributions(new ArrayList<>());
        if (Objects.nonNull(record.getAcronym()) && !record.getAcronym().isEmpty()) {
            dto.setNameAbbreviation(multilingualContentConverter.toDTO(record.getAcronym()));
        } else {
            dto.setNameAbbreviation(new ArrayList<>());
        }

        dto.setSubtitle(new ArrayList<>());

        var languageCode = record.getLanguage().trim().toUpperCase();
        if (languageCode.isEmpty()) {
            languageCode = LanguageAbbreviations.ENGLISH;
        }

        if (languageCode.equals("GE")) {
            languageCode = LanguageAbbreviations.GERMAN;
        }

        var languageTag = languageService.findLanguageByCode(languageCode);
        if (Objects.nonNull(languageTag.getId())) {
            dto.setLanguageIds(Set.of(languageTag.getId()));
        } else {
            dto.setLanguageIds(new HashSet<>());
        }

        return dto;
    }
}
