package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PersonContributionConverter;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.model.document.Document;

@Transactional
public class DocumentPublicationConverter {

    public static DocumentDTO toDTO(Document document) {
        var dto = new DocumentDTO();
        setCommonFields(document, dto);
        return dto;
    }

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
        publicationDTO.setOpenAlexId(publication.getOpenAlexId());
        publicationDTO.setWebOfScienceId(publication.getWebOfScienceId());

        publicationDTO.setIsMetadataValid(publication.getIsMetadataValid());
        publicationDTO.setAreFilesValid(publication.getAreFilesValid());
        publicationDTO.setIsArchived(publication.getIsArchived());

        if (Objects.nonNull(publication.getEvent())) {
            publicationDTO.setEventId(publication.getEvent().getId());
        }

        publication.getFileItems().forEach(fileItem -> {
            publicationDTO.getFileItems().add(DocumentFileConverter.toDTO(fileItem));
        });

        publication.getProofs().forEach(proof -> {
            publicationDTO.getProofs().add(DocumentFileConverter.toDTO(proof));
        });
    }
}
