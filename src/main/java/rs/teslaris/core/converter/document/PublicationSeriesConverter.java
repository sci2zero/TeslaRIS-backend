package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PersonContributionConverter;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.PublicationSeries;

public class PublicationSeriesConverter {

    public static JournalResponseDTO toDTO(Journal journal) {
        var responseDTO = new JournalResponseDTO();

        convertCommonFields(journal, responseDTO);

        responseDTO.setLanguageTagNames(new ArrayList<>());
        journal.getLanguages()
            .forEach(languageTag -> responseDTO.getLanguageTagNames().add(
                languageTag.getLanguageTag()));

        return responseDTO;
    }

    public static BookSeriesResponseDTO toDTO(BookSeries bookSeries) {
        var responseDTO = new BookSeriesResponseDTO();

        convertCommonFields(bookSeries, responseDTO);

        responseDTO.setLanguageTagNames(new ArrayList<>());
        bookSeries.getLanguages()
            .forEach(languageTag -> responseDTO.getLanguageTagNames().add(
                languageTag.getLanguageTag()));

        return responseDTO;
    }

    private static void convertCommonFields(PublicationSeries publicationSeries,
                                            PublicationSeriesDTO responseDTO) {
        responseDTO.setId(publicationSeries.getId());
        responseDTO.setTitle(
            MultilingualContentConverter.getMultilingualContentDTO(publicationSeries.getTitle()));
        responseDTO.setSubtitle(
            MultilingualContentConverter.getMultilingualContentDTO(
                publicationSeries.getSubtitle()));
        responseDTO.setEissn(publicationSeries.getEISSN());
        responseDTO.setPrintISSN(publicationSeries.getPrintISSN());
        responseDTO.setOpenAlexId(publicationSeries.getOpenAlexId());
        responseDTO.setNameAbbreviation(
            MultilingualContentConverter.getMultilingualContentDTO(
                publicationSeries.getNameAbbreviation()));

        responseDTO.setLanguageTagIds(new ArrayList<>());
        publicationSeries.getLanguages()
            .forEach(languageTag -> responseDTO.getLanguageTagIds().add(languageTag.getId()));

        responseDTO.setContributions(
            PersonContributionConverter.publicationSeriesContributionToDTO(
                publicationSeries.getContributions()));

        responseDTO.setUris(publicationSeries.getUris());
    }
}
