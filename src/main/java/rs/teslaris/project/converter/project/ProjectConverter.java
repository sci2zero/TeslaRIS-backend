package rs.teslaris.project.converter.project;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.Project;

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

        return dto;
    }

}
