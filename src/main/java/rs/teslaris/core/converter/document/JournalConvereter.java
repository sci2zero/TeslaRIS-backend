package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PersonContributionConverter;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.model.document.Journal;

public class JournalConvereter {

    public static JournalResponseDTO toDTO(Journal journal) {
        var responseDTO = new JournalResponseDTO();
        responseDTO.setId(journal.getId());
        responseDTO.setTitle(
            MultilingualContentConverter.getMultilingualContentDTO(journal.getTitle()));
        responseDTO.setEISSN(journal.getEISSN());
        responseDTO.setPrintISSN(journal.getPrintISSN());
        responseDTO.setNameAbbreviation(
            MultilingualContentConverter.getMultilingualContentDTO(journal.getNameAbbreviation()));

        responseDTO.setLanguageTagIds(new ArrayList<>());
        journal.getLanguages()
            .forEach(languageTag -> responseDTO.getLanguageTagIds().add(languageTag.getId()));

        responseDTO.setLanguageTagNames(new ArrayList<>());
        journal.getLanguages().forEach(languageTag -> responseDTO.getLanguageTagNames().add(
            languageTag.getLanguageTag()));

        responseDTO.setContributions(
            PersonContributionConverter.journalContributionToDTO(journal.getContributions()));

        return responseDTO;
    }
}
