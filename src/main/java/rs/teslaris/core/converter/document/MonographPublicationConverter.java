package rs.teslaris.core.converter.document;

import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.model.document.MonographPublication;

public class MonographPublicationConverter extends DocumentPublicationConverter {

    public static MonographPublicationDTO toDTO(MonographPublication monographPublication) {
        var response = new MonographPublicationDTO();

        setCommonFields(monographPublication, response);
        setMonographPublicationAffiliatedFields(monographPublication, response);

        return response;
    }

    private static void setMonographPublicationAffiliatedFields(
        MonographPublication monographPublication,
        MonographPublicationDTO monographPublicationDTO) {
        monographPublicationDTO.setMonographPublicationType(
            monographPublication.getMonographPublicationType());
        monographPublicationDTO.setStartPage(monographPublication.getStartPage());
        monographPublicationDTO.setEndPage(monographPublication.getEndPage());
        monographPublicationDTO.setNumberOfPages(monographPublication.getNumberOfPages());
        monographPublicationDTO.setArticleNumber(monographPublication.getArticleNumber());
        monographPublicationDTO.setMonographId(monographPublication.getMonograph().getId());
    }
}