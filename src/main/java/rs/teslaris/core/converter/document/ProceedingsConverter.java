package rs.teslaris.core.converter.document;

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
        proceedingsResponseDTO.setEditionTitle(proceedings.getEditionTitle());
        proceedingsResponseDTO.setEditionNumber(proceedings.getEditionNumber());
        proceedingsResponseDTO.setEditionISSN(proceedings.getEditionISSN());

        setLanguageInfo(proceedings, proceedingsResponseDTO);
        setJournalInfo(proceedings, proceedingsResponseDTO);
        setEventInfo(proceedings, proceedingsResponseDTO);
        setPublisherInfo(proceedings, proceedingsResponseDTO);
    }

    private static void setLanguageInfo(Proceedings proceedings,
                                        ProceedingsResponseDTO proceedingsResponseDTO) {
        proceedings.getLanguages().forEach(languageTag -> {
            proceedingsResponseDTO.getLanguageTagIds().add(languageTag.getId());
        });
    }

    private static void setJournalInfo(Proceedings proceedings,
                                       ProceedingsResponseDTO proceedingsResponseDTO) {
        var journal = proceedings.getJournal();
        proceedingsResponseDTO.setJournalId(journal != null ? journal.getId() : 0);
        proceedingsResponseDTO.setJournalVolume(proceedings.getJournalVolume());
        proceedingsResponseDTO.setJournalIssue(proceedings.getJournalIssue());
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
