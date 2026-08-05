package rs.teslaris.project.converter.project;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.Project;

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

        project.getOrganisations().forEach(organisation ->
            dto.getOrganisationIds().add(organisation.getId()));

        dto.setUris(project.getUris());
        dto.setDateFrom(project.getDateFrom());
        dto.setDateTo(project.getDateTo());
        dto.setStatus(project.getStatus());
        dto.setCollaborationType(project.getCollaborationType());
        dto.setResearchType(project.getResearchType());
        dto.setNotFunded(project.getNotFunded());

        dto.setCosts(new MonetaryAmountDTO());
        if (Objects.nonNull(project.getCosts())) {
            dto.getCosts().setAmount(project.getCosts().getAmount());
            dto.getCosts().setCurrencyId(project.getCosts().getCurrency().getId());
        }

        project.getPersons().forEach(member ->
            dto.getPersons().add(PersonProjectContributionConverter.toDTO(member)));

        project.getRelatedProjects().forEach(relation ->
            dto.getRelations().add(ProjectsRelationConverter.toDTO(relation)));

        return dto;
    }

}
