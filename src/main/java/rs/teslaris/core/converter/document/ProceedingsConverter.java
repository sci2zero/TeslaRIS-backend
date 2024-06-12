package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.model.document.Proceedings;

public class ProceedingsConverter extends DocumentPublicationConverter {

    public static ProceedingsResponseDTO toDTO(Proceedings proceedings) {
        var response = new ProceedingsResponseDTO();

        setCommonFields(proceedings, response);
        setProceedingsAffiliatedFields(proceedings, response);

        return response;
    }

    private static void setProceedingsAffiliatedFields(Proceedings proceedings,
                                                       ProceedingsResponseDTO proceedingsResponseDTO) {
        proceedingsResponseDTO.setEISBN(proceedings.getEISBN());
        proceedingsResponseDTO.setPrintISBN(proceedings.getPrintISBN());
        proceedingsResponseDTO.setNumberOfPages(proceedings.getNumberOfPages());
        proceedingsResponseDTO.setPublicationSeriesIssue(proceedings.getPublicationSeriesIssue());
        proceedingsResponseDTO.setPublicationSeriesVolume(proceedings.getPublicationSeriesVolume());

        proceedingsResponseDTO.setLanguageTagIds(new ArrayList<>());
        setLanguageInfo(proceedings, proceedingsResponseDTO);
        setPublicationSeriesInfo(proceedings, proceedingsResponseDTO);
        setEventInfo(proceedings, proceedingsResponseDTO);
        setPublisherInfo(proceedings, proceedingsResponseDTO);
    }

    private static void setLanguageInfo(Proceedings proceedings,
                                        ProceedingsResponseDTO proceedingsResponseDTO) {
        proceedings.getLanguages().forEach(languageTag -> {
            proceedingsResponseDTO.getLanguageTagIds().add(languageTag.getId());
        });
    }

    private static void setPublicationSeriesInfo(Proceedings proceedings,
                                                 ProceedingsResponseDTO proceedingsResponseDTO) {
        var publicationSeries = proceedings.getPublicationSeries();
        proceedingsResponseDTO.setPublicationSeriesId(
            publicationSeries != null ? publicationSeries.getId() : 0);
    }

    private static void setEventInfo(Proceedings proceedings,
                                     ProceedingsResponseDTO proceedingsResponseDTO) {
        var event = proceedings.getEvent();
        proceedingsResponseDTO.setEventId(event.getId());
        proceedingsResponseDTO.setEventName(
            MultilingualContentConverter.getMultilingualContentDTO(event.getName()));
    }

    private static void setPublisherInfo(Proceedings proceedings,
                                         ProceedingsResponseDTO proceedingsResponseDTO) {
        var publisher = proceedings.getPublisher();
        if (publisher == null) {
            return;
        }

        proceedingsResponseDTO.setPublisherId(publisher.getId());
        proceedingsResponseDTO.setPublisherName(
            MultilingualContentConverter.getMultilingualContentDTO(publisher.getName()));
    }
}
