package rs.teslaris.core.converter.document;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.model.document.JournalPublication;

public class JournalPublicationConverter extends DocumentPublicationConverter {

    public static JournalPublicationResponseDTO toDTO(JournalPublication publication) {
        var publicationDTO = new JournalPublicationResponseDTO();

        setCommonFields(publication, publicationDTO);
        setJournalAffiliatedFields(publication, publicationDTO);

        return publicationDTO;
    }

    private static void setJournalAffiliatedFields(JournalPublication publication,
                                                   JournalPublicationResponseDTO publicationDTO) {
        publicationDTO.setJournalPublicationType(publication.getJournalPublicationType());
        publicationDTO.setStartPage(publication.getStartPage());
        publicationDTO.setEndPage(publication.getEndPage());
        publicationDTO.setNumberOfPages(publication.getNumberOfPages());
        publicationDTO.setArticleNumber(publication.getArticleNumber());
        publicationDTO.setVolume(publication.getVolume());
        publicationDTO.setIssue(publication.getIssue());
        publicationDTO.setJournalId(publication.getJournal().getId());
        publicationDTO.setJournalName(
            MultilingualContentConverter.getMultilingualContentDTO(
                publication.getJournal().getTitle()));
    }
}
