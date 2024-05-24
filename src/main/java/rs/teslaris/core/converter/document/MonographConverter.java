package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import java.util.Objects;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.Monograph;

public class MonographConverter extends DocumentPublicationConverter {

    public static MonographDTO toDTO(Monograph monograph) {
        var response = new MonographDTO();

        setCommonFields(monograph, response);
        setMonographAffiliatedFields(monograph, response);

        return response;
    }

    private static void setMonographAffiliatedFields(Monograph monograph,
                                                     MonographDTO monographResponseDTO) {
        monographResponseDTO.setEISBN(monograph.getEISBN());
        monographResponseDTO.setPrintISBN(monograph.getPrintISBN());
        monographResponseDTO.setNumberOfPages(monograph.getNumberOfPages());

        monographResponseDTO.setLanguageTagIds(new ArrayList<>());
        setLanguageInfo(monograph, monographResponseDTO);
        setPublicationSeriesInfo(monograph, monographResponseDTO);
    }

    private static void setLanguageInfo(Monograph monograph,
                                        MonographDTO monographResponseDTO) {
        monograph.getLanguages().forEach(languageTag -> {
            monographResponseDTO.getLanguageTagIds().add(languageTag.getId());
        });
    }

    private static void setPublicationSeriesInfo(Monograph monograph,
                                                 MonographDTO monographResponseDTO) {
        var publicationSeries = monograph.getPublicationSeries();

        if (Objects.isNull(publicationSeries)) {
            return;
        }

        if (publicationSeries instanceof BookSeries) {
            monographResponseDTO.setBookSeriesId(publicationSeries.getId());
        } else if (publicationSeries instanceof Journal) {
            monographResponseDTO.setJournalId(publicationSeries.getId());
        }
    }
}
