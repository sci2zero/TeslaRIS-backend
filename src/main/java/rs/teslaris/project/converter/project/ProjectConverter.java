package rs.teslaris.project.converter.project;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.project.converter.funding.FundingPartConverter;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.Project;

import java.util.Comparator;
import java.util.Objects;

public class ProjectConverter {

    public static ProjectDTO toDTO(Project project) {
        var dto = new ProjectDTO();

        dto.setId(project.getId());
        dto.setInternalIdentifiers(project.getInternalIdentifiers());
        dto.setOldIds(project.getOldIds());
        dto.setMergedIds(project.getMergedIds());
        dto.setDoi(project.getDoi());
        dto.setRaid(project.getRaid());
        dto.setNationalId(project.getNationalId());

        dto.setName(
                MultilingualContentConverter.getMultilingualContentDTO(project.getName()));
        dto.setDescription(
                MultilingualContentConverter.getMultilingualContentDTO(project.getDescription()));
        dto.setNameAbbreviation(
                MultilingualContentConverter.getMultilingualContentDTO(project.getNameAbbreviation()));
        dto.setKeywords(
                MultilingualContentConverter.getMultilingualContentDTO(project.getKeywords()));

        project.getResearchAreas().forEach(researchArea ->
                dto.getResearchAreasId().add(researchArea.getId()));

        project.getOrganisations().stream()
                .sorted(Comparator.comparing(OrganisationUnitProjectContribution::getOrderNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(organisation ->
                        dto.getConsortium().add(toConsortiumMemberDTO(organisation)));

        dto.setUris(project.getUris());
        dto.setDateFrom(project.getDateFrom());
        dto.setDateTo(project.getDateTo());
        dto.setStatus(project.getStatus());
        dto.setCollaborationType(project.getCollaborationType());
        dto.setResearchType(project.getResearchType());
        dto.setNotFunded(project.getNotFunded());

        if (Objects.nonNull(project.getCosts())) {
            dto.setCosts(new MonetaryAmountDTO());
            dto.getCosts().setAmount(project.getCosts().getAmount());
            dto.getCosts().setCurrencyId(project.getCosts().getCurrency().getId());
            dto.getCosts().setCurrencyCode(project.getCosts().getCurrency().getCode());
            dto.getCosts().setCurrencySymbol(project.getCosts().getCurrency().getSymbol());
        }

        project.getPersons().forEach(member ->
                dto.getPersons().add(PersonProjectContributionConverter.toDTO(member)));

        project.getRelatedProjects().forEach(relation ->
                dto.getRelations().add(ProjectsRelationConverter.toDTO(relation)));

        return dto;
    }

    private static OrganisationUnitProjectContributionDTO toConsortiumMemberDTO(
            OrganisationUnitProjectContribution contribution) {
        var dto = new OrganisationUnitProjectContributionDTO();

        dto.setId(contribution.getId());

        if (Objects.nonNull(contribution.getOrganisationUnit())) {
            dto.setOrganisationUnitId(contribution.getOrganisationUnit().getId());
            dto.setOrganisationUnitName(MultilingualContentConverter.getMultilingualContentDTO(
                    contribution.getOrganisationUnit().getName()));
        }

        dto.setDisplayOrganisationUnit(MultilingualContentConverter.getMultilingualContentDTO(
                contribution.getDisplayOrganisationUnit()));
        dto.setContributionDescription(MultilingualContentConverter.getMultilingualContentDTO(
                contribution.getContributionDescription()));
        dto.setContributionType(contribution.getContributionType());
        dto.setOrderNumber(contribution.getOrderNumber());
        dto.setDateFrom(contribution.getDateFrom());
        dto.setDateTo(contribution.getDateTo());
        dto.setUris(contribution.getUris());
        dto.setIsMainContributor(contribution.isMainContributor());
        dto.setFavorite(contribution.getFavorite());

        if (Objects.nonNull(contribution.getContactPerson())) {
            dto.setContactPersonId(contribution.getContactPerson().getId());
        }

        contribution.getFundingParts().forEach(fundingPart ->
                dto.getFundingParts().add(FundingPartConverter.toDTO(fundingPart)));

        dto.setDisplayProject(MultilingualContentConverter.getMultilingualContentDTO(
                contribution.getDisplayProject()));

        return dto;
    }

}
