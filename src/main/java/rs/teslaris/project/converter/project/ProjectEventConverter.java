package rs.teslaris.project.converter.project;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.project.converter.funding.FundingPartConverter;
import rs.teslaris.project.dto.project.ProjectEventDTO;
import rs.teslaris.project.model.project.ProjectEvent;

import java.util.Objects;

public class ProjectEventConverter {

    public static ProjectEventDTO toDTO(ProjectEvent projectEvent) {
        var dto = new ProjectEventDTO();

        dto.setId(projectEvent.getId());

        if (Objects.nonNull(projectEvent.getProject())) {
            dto.setProjectId(projectEvent.getProject().getId());
        }

        if (Objects.nonNull(projectEvent.getEvent())) {
            dto.setEventId(projectEvent.getEvent().getId());
        }

        projectEvent.getFundingParts().forEach(
                fundingPart -> dto.getFundingParts().add(FundingPartConverter.toDTO(fundingPart)));
        dto.setTextualDescription(MultilingualContentConverter.getMultilingualContentDTO(
                projectEvent.getTextualDescription()));
        dto.setRelationType(projectEvent.getRelationType());

        return dto;
    }

}
