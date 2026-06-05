package rs.teslaris.project.converter.project;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.converter.person.PersonNameConverter;
import rs.teslaris.core.converter.person.PostalAddressConverter;
import rs.teslaris.project.converter.funding.FundingPartConverter;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.model.project.PersonProjectContribution;

import java.util.Objects;

public class PersonProjectContributionConverter {

    public static PersonProjectContributionDTO toDTO(PersonProjectContribution contribution) {

        var dto = new PersonProjectContributionDTO();

        dto.setId(contribution.getId());
        dto.setOrderNumber(contribution.getOrderNumber());

        if (Objects.nonNull(contribution.getPerson())) {
            dto.setPersonId(contribution.getPerson().getId());
        }

        dto.setContributionDescription(
                MultilingualContentConverter.getMultilingualContentDTO(
                        contribution.getContributionDescription()));

        contribution.getInstitutions().forEach(institution ->
                dto.getInstitutionIds().add(institution.getId()));

        var affiliation = contribution.getAffiliationStatement();
        if (Objects.nonNull(affiliation)) {
            dto.setDisplayAffiliationStatement(
                    MultilingualContentConverter.getMultilingualContentDTO(
                            affiliation.getDisplayAffiliationStatement()));

            if (Objects.nonNull(affiliation.getDisplayPersonName())) {
                dto.setPersonName(PersonNameConverter.toDTO(affiliation.getDisplayPersonName()));
            }
            if (Objects.nonNull(affiliation.getPostalAddress())) {
                dto.setPostalAddress(PostalAddressConverter.toDto(affiliation.getPostalAddress()));
            }
            if (Objects.nonNull(affiliation.getContact())) {
                dto.setContact(ContactConverter.toDTO(affiliation.getContact()));
            }
        }

        dto.setContributionType(contribution.getContributionType());
        dto.setInvestigationRole(contribution.getInvestigationRole());

        dto.setOtherRoleDescription(
                MultilingualContentConverter.getMultilingualContentDTO(
                        contribution.getOtherRoleDescription()));

        contribution.getFundingParts().forEach(fundingPart ->
                dto.getFundingParts().add(FundingPartConverter.toDTO(fundingPart)));

        return dto;
    }
}
