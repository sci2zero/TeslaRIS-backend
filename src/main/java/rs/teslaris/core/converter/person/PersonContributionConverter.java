package rs.teslaris.core.converter.person;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.PersonEventContributionDTO;
import rs.teslaris.core.dto.document.PersonPublicationSeriesContributionDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.PersonEventContribution;
import rs.teslaris.core.model.document.PersonPublicationSeriesContribution;

public class PersonContributionConverter {

    public static ArrayList<PersonDocumentContributionDTO> documentContributionToDTO(
        Set<PersonDocumentContribution> contributions) {
        var contributionDTOs = new ArrayList<PersonDocumentContributionDTO>();
        contributions.stream().filter(c -> c.getApproveStatus().equals(ApproveStatus.APPROVED) &&
                c.getDeleted().equals(false))
            .forEach((c) -> {
                var contribution = new PersonDocumentContributionDTO();
                setCommonFields(contribution, c);

                contribution.setContributionType(c.getContributionType());
                contribution.setIsMainContributor(c.getIsMainContributor());
                contribution.setIsCorrespondingContributor(c.getIsCorrespondingContributor());
                contribution.setIsBoardPresident(c.getIsBoardPresident());
                contribution.setPersonalTitle(c.getPersonalTitle());
                contribution.setEmploymentTitle(c.getEmploymentTitle());

                contributionDTOs.add(contribution);
            });
        return contributionDTOs;
    }

    public static ArrayList<PersonPublicationSeriesContributionDTO> publicationSeriesContributionToDTO(
        Set<PersonPublicationSeriesContribution> contributions) {
        var contributionDTOs = new ArrayList<PersonPublicationSeriesContributionDTO>();
        contributions.stream().filter(c -> c.getApproveStatus().equals(ApproveStatus.APPROVED))
            .forEach((c) -> {
                var contribution = new PersonPublicationSeriesContributionDTO();
                setCommonFields(contribution, c);

                contribution.setContributionType(c.getContributionType());
                contribution.setDateFrom(c.getDateFrom());
                contribution.setDateTo(c.getDateTo());

                contributionDTOs.add(contribution);
            });
        return contributionDTOs;
    }

    public static ArrayList<PersonEventContributionDTO> eventContributionToDTO(
        Set<PersonEventContribution> contributions) {
        var contributionDTOs = new ArrayList<PersonEventContributionDTO>();
        contributions.stream().filter(c -> c.getApproveStatus().equals(ApproveStatus.APPROVED))
            .forEach((c) -> {
                var contribution = new PersonEventContributionDTO();
                setCommonFields(contribution, c);

                contribution.setEventContributionType(c.getContributionType());

                contributionDTOs.add(contribution);
            });
        return contributionDTOs;
    }

    private static void setCommonFields(PersonContributionDTO contributionDTO,
                                        PersonContribution contribution) {
        contributionDTO.setId(contribution.getId());

        contributionDTO.setContributionDescription(
            MultilingualContentConverter.getMultilingualContentDTO(
                contribution.getContributionDescription()));
        contributionDTO.setDisplayAffiliationStatement(
            MultilingualContentConverter.getMultilingualContentDTO(
                contribution.getAffiliationStatement().getDisplayAffiliationStatement()));

        contributionDTO.setOrderNumber(contribution.getOrderNumber());

        if (Objects.nonNull(contribution.getPerson())) {
            contributionDTO.setPersonId(contribution.getPerson().getId());
        }

        contributionDTO.setInstitutionIds(new ArrayList<>());
        contribution.getInstitutions()
            .forEach(i -> {
                contributionDTO.getInstitutionIds().add(i.getId());
                contributionDTO.getDisplayInstitutionNames()
                    .add(MultilingualContentConverter.getMultilingualContentDTO(i.getName()));
            });

        contributionDTO.setPersonName(PersonNameConverter.toDTO(
            contribution.getAffiliationStatement().getDisplayPersonName()));
        contributionDTO.setPostalAddress(PostalAddressConverter.toDto(
            contribution.getAffiliationStatement().getPostalAddress()));
        contributionDTO.setContact(
            ContactConverter.toDTO(contribution.getAffiliationStatement().getContact()));
    }
}
