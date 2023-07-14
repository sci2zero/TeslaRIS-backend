package rs.teslaris.core.converter.document;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PersonContributionConverter;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.model.document.Document;

public class DocumentPublicationConverter {

    protected static void setCommonFields(Document publication, DocumentDTO publicationDTO) {
        publicationDTO.setId(publication.getId());
        publicationDTO.setTitle(
            MultilingualContentConverter.getMultilingualContentDTO(publication.getTitle()));
        publicationDTO.setSubTitle(
            MultilingualContentConverter.getMultilingualContentDTO(publication.getSubTitle()));
        publicationDTO.setDescription(
            MultilingualContentConverter.getMultilingualContentDTO(publication.getDescription()));
        publicationDTO.setKeywords(
            MultilingualContentConverter.getMultilingualContentDTO(publication.getKeywords()));

        publicationDTO.setContributions(
            PersonContributionConverter.documentContributionToDTO(publication.getContributors()));

        publicationDTO.setUris(publication.getUris());
        publicationDTO.setDocumentDate(publication.getDocumentDate());
        publicationDTO.setDoi(publication.getDoi());
        publicationDTO.setScopusId(publication.getScopusId());
    }
}
