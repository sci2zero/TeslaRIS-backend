package rs.teslaris.project.converter.project;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.project.converter.funding.FundingPartConverter;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;

import java.util.Objects;

public class OrganisationUnitProjectContributionConverter {

    public static OrganisationUnitProjectContributionDTO toDTO(
            OrganisationUnitProjectContribution organisation) {
        var dto = new OrganisationUnitProjectContributionDTO();

        dto.setId(organisation.getId());

        if (Objects.nonNull(organisation.getOrganisationUnit())) {
            dto.setOrganisationUnitId(organisation.getOrganisationUnit().getId());
            dto.setOrganisationUnitName(MultilingualContentConverter.getMultilingualContentDTO(
                    organisation.getOrganisationUnit().getName()));
        }

        dto.setDisplayOrganisationUnit(MultilingualContentConverter.getMultilingualContentDTO(
                organisation.getDisplayOrganisationUnit()));
        dto.setContributionDescription(MultilingualContentConverter.getMultilingualContentDTO(
                organisation.getContributionDescription()));
        dto.setContributionType(organisation.getContributionType());
        dto.setOrderNumber(organisation.getOrderNumber());
        dto.setDateFrom(organisation.getDateFrom());
        dto.setDateTo(organisation.getDateTo());
        dto.setUris(organisation.getUris());
        dto.setIsMainContributor(organisation.isMainContributor());
        dto.setFavorite(organisation.getFavorite());

        if (Objects.nonNull(organisation.getContactPerson())) {
            dto.setContactPersonId(organisation.getContactPerson().getId());
        }

        organisation.getFundingParts().forEach(fundingPart ->
                dto.getFundingParts().add(FundingPartConverter.toDTO(fundingPart)));

        dto.setDisplayProject(MultilingualContentConverter.getMultilingualContentDTO(
                organisation.getDisplayProject()));

        return dto;
    }

}
