package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.converter.person.PersonNameConverter;
import rs.teslaris.core.converter.person.PostalAddressConverter;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.JournalPublication;

public class JournalPublicationConverter {

    public static JournalPublicationResponseDTO toDTO(JournalPublication publication) {
        var publicationDTO = new JournalPublicationResponseDTO();

        setCommonFields(publication, publicationDTO);
        setJournalAffiliatedFields(publication, publicationDTO);

        return publicationDTO;
    }

    private static void setCommonFields(JournalPublication publication,
                                        JournalPublicationResponseDTO publicationDTO) {
        publicationDTO.setId(publication.getId());
        publicationDTO.setTitle(
            MultilingualContentConverter.getMultilingualContentDTO(
                publication.getTitle()));
        publicationDTO.setSubTitle(
            MultilingualContentConverter.getMultilingualContentDTO(
                publication.getSubTitle()));
        publicationDTO.setDescription(
            MultilingualContentConverter.getMultilingualContentDTO(
                publication.getDescription()));
        publicationDTO.setKeywords(
            MultilingualContentConverter.getMultilingualContentDTO(
                publication.getKeywords()));

        setContributions(publication, publicationDTO);

        publicationDTO.setUris(publication.getUris());
        publicationDTO.setDocumentDate(publication.getDocumentDate());
        publicationDTO.setDoi(publication.getDoi());
        publicationDTO.setScopusId(publication.getScopusId());
    }

    private static void setContributions(JournalPublication publication,
                                         JournalPublicationResponseDTO publicationDTO) {
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
