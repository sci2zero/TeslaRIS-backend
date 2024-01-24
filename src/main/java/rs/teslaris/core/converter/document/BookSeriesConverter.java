package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PersonContributionConverter;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.model.document.Journal;

public class BookSeriesConverter {

    public static JournalResponseDTO toDTO(Journal journal) {
        var responseDTO = new JournalResponseDTO();
        responseDTO.setId(journal.getId());
        responseDTO.setTitle(
            MultilingualContentConverter.getMultilingualContentDTO(journal.getTitle()));
        responseDTO.setEissn(journal.getEISSN());
        responseDTO.setPrintISSN(journal.getPrintISSN());
        responseDTO.setNameAbbreviation(
            MultilingualContentConverter.getMultilingualContentDTO(
                journal.getNameAbbreviation()));

        responseDTO.setLanguageTagIds(new ArrayList<>());
        journal.getLanguages()
            .forEach(languageTag -> responseDTO.getLanguageTagIds().add(languageTag.getId()));

        responseDTO.setLanguageTagNames(new ArrayList<>());
        journal.getLanguages()
            .forEach(languageTag -> responseDTO.getLanguageTagNames().add(
                languageTag.getLanguageTag()));

        responseDTO.setContributions(
            PersonContributionConverter.publicationSeriesContributionToDTO(
                journal.getContributions()));

        return responseDTO;
    }

    public static BookSeriesResponseDTO toDTO(BookSeries bookSeries) {
        var responseDTO = new BookSeriesResponseDTO();
        responseDTO.setId(bookSeries.getId());
        responseDTO.setTitle(
            MultilingualContentConverter.getMultilingualContentDTO(bookSeries.getTitle()));
        responseDTO.setEISSN(bookSeries.getEISSN());
        responseDTO.setPrintISSN(bookSeries.getPrintISSN());
        responseDTO.setNameAbbreviation(
            MultilingualContentConverter.getMultilingualContentDTO(
                bookSeries.getNameAbbreviation()));

        responseDTO.setLanguageTagIds(new ArrayList<>());
        bookSeries.getLanguages()
            .forEach(languageTag -> responseDTO.getLanguageTagIds().add(languageTag.getId()));

        responseDTO.setLanguageTagNames(new ArrayList<>());
        bookSeries.getLanguages()
            .forEach(languageTag -> responseDTO.getLanguageTagNames().add(
                languageTag.getLanguageTag()));

        responseDTO.setContributions(
            PersonContributionConverter.publicationSeriesContributionToDTO(
                bookSeries.getContributions()));

        return responseDTO;
    }
}
