package rs.teslaris.core.converter.document;

import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.model.document.ProceedingsPublication;

public class ProceedingsPublicationConverter extends DocumentPublicationConverter {

    public static ProceedingsPublicationDTO toDTO(ProceedingsPublication publication) {
        var publicationDTO = new ProceedingsPublicationDTO();

        setCommonFields(publication, publicationDTO);
        setProceedingsAffiliatedFields(publication, publicationDTO);

        return publicationDTO;
    }

    private static void setProceedingsAffiliatedFields(ProceedingsPublication publication,
                                                       ProceedingsPublicationDTO publicationDTO) {
        publicationDTO.setProceedingsPublicationType(publication.getProceedingsPublicationType());
        publicationDTO.setStartPage(publication.getStartPage());
        publicationDTO.setEndPage(publication.getEndPage());
        publicationDTO.setNumberOfPages(publication.getNumberOfPages());
        publicationDTO.setArticleNumber(publication.getArticleNumber());
        publicationDTO.setProceedingsId(publication.getProceedings().getId());
        publicationDTO.setEventId(publication.getEvent().getId());
    }
}
