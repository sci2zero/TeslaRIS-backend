package rs.teslaris.core.converter.person;

import java.util.ArrayList;
import java.util.Set;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.PersonEventContributionDTO;
import rs.teslaris.core.dto.document.PersonJournalContributionDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.PersonEventContribution;
import rs.teslaris.core.model.document.PersonJournalContribution;

public class PersonContributionConverter {

    public static ArrayList<PersonDocumentContributionDTO> documentContributionToDTO(
        Set<PersonDocumentContribution> contributions) {
        var contributionDTOs = new ArrayList<PersonDocumentContributionDTO>();
        contributions.stream().filter(c -> c.getApproveStatus().equals(ApproveStatus.APPROVED))
            .forEach((c) -> {
                var contribution = new PersonDocumentContributionDTO();
                setCommonFields(contribution, c);

                contribution.setContributionType(c.getContributionType());
                contribution.setIsMainContributor(c.isMainContributor());
                contribution.setIsCorrespondingContributor(c.isCorrespondingContributor());

                contributionDTOs.add(contribution);
            });
        return contributionDTOs;
    }

    public static ArrayList<PersonJournalContributionDTO> journalContributionToDTO(
        Set<PersonJournalContribution> contributions) {
        var contributionDTOs = new ArrayList<PersonJournalContributionDTO>();
        contributions.stream().filter(c -> c.getApproveStatus().equals(ApproveStatus.APPROVED))
            .forEach((c) -> {
                var contribution = new PersonJournalContributionDTO();
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
        contributionDTO.setContributionDescription(
            MultilingualContentConverter.getMultilingualContentDTO(
                contribution.getContributionDescription()));
        contributionDTO.setDisplayAffiliationStatement(
            MultilingualContentConverter.getMultilingualContentDTO(
                contribution.getAffiliationStatement().getDisplayAffiliationStatement()));

        contributionDTO.setOrderNumber(contribution.getOrderNumber());

        contributionDTO.setInstitutionIds(new ArrayList<>());
        contribution.getInstitutions()
            .forEach(i -> contributionDTO.getInstitutionIds().add(i.getId()));

        contributionDTO.setPersonName(PersonNameConverter.toDTO(
            contribution.getAffiliationStatement().getDisplayPersonName()));
        contributionDTO.setPostalAddress(PostalAddressConverter.toDto(
            contribution.getAffiliationStatement().getPostalAddress()));
        contributionDTO.setContact(
            ContactConverter.toDTO(contribution.getAffiliationStatement().getContact()));
    }
}
