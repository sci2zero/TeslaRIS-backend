package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.converter.person.PersonNameConverter;
import rs.teslaris.core.converter.person.PostalAddressConverter;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
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

        setContributions(publication, publicationDTO);

        publicationDTO.setUris(publication.getUris());
        publicationDTO.setDocumentDate(publication.getDocumentDate());
        publicationDTO.setDoi(publication.getDoi());
        publicationDTO.setScopusId(publication.getScopusId());
    }

    protected static void setContributions(Document publication, DocumentDTO publicationDTO) {
        var contributions = new ArrayList<PersonDocumentContributionDTO>();
        publication.getContributors().stream()
            .filter(c -> c.getApproveStatus().equals(ApproveStatus.APPROVED)).forEach((c) -> {
                var contribution = new PersonDocumentContributionDTO();
                contribution.setContributionDescription(
                    MultilingualContentConverter.getMultilingualContentDTO(
                        c.getContributionDescription()));
                contribution.setDisplayAffiliationStatement(
                    MultilingualContentConverter.getMultilingualContentDTO(
                        c.getAffiliationStatement().getDisplayAffiliationStatement()));

                contribution.setOrderNumber(c.getOrderNumber());

                contribution.setInstitutionIds(new ArrayList<>());
                c.getInstitutions().forEach(i -> contribution.getInstitutionIds().add(i.getId()));

                contribution.setPersonName(
                    PersonNameConverter.toDTO(c.getAffiliationStatement().getDisplayPersonName()));
                contribution.setPostalAddress(
                    PostalAddressConverter.toDto(c.getAffiliationStatement().getPostalAddress()));
                contribution.setContact(
                    ContactConverter.toDTO(c.getAffiliationStatement().getContact()));

                contribution.setContributionType(c.getContributionType());
                contribution.setIsMainContributor(c.isMainContributor());
                contribution.setIsCorrespondingContributor(c.isCorrespondingContributor());

                contributions.add(contribution);
            });
        publicationDTO.setContributions(contributions);
    }
}
