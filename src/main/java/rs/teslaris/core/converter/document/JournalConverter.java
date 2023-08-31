package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PersonContributionConverter;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.model.document.PublicationSeries;

public class JournalConverter {

    public static JournalResponseDTO toDTO(PublicationSeries publicationSeries) {
        var responseDTO = new JournalResponseDTO();
        responseDTO.setId(publicationSeries.getId());
        responseDTO.setTitle(
            MultilingualContentConverter.getMultilingualContentDTO(publicationSeries.getTitle()));
        responseDTO.setEISSN(publicationSeries.getEISSN());
        responseDTO.setPrintISSN(publicationSeries.getPrintISSN());
        responseDTO.setNameAbbreviation(
            MultilingualContentConverter.getMultilingualContentDTO(
                publicationSeries.getNameAbbreviation()));

        responseDTO.setLanguageTagIds(new ArrayList<>());
        publicationSeries.getLanguages()
            .forEach(languageTag -> responseDTO.getLanguageTagIds().add(languageTag.getId()));

        responseDTO.setLanguageTagNames(new ArrayList<>());
        publicationSeries.getLanguages()
            .forEach(languageTag -> responseDTO.getLanguageTagNames().add(
                languageTag.getLanguageTag()));

        responseDTO.setContributions(
            PersonContributionConverter.journalContributionToDTO(
                publicationSeries.getContributions()));

        return responseDTO;
    }
}
