package rs.teslaris.project.converter.project;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.project.converter.funding.FundingPartConverter;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.model.project.ProjectDocument;

import java.util.Objects;

public class ProjectDocumentConverter {

    public static ProjectDocumentDTO toDTO(ProjectDocument projectDocument) {
        var dto = new ProjectDocumentDTO();

        dto.setId(projectDocument.getId());

        if (Objects.nonNull(projectDocument.getProject())) {
            dto.setProjectId(projectDocument.getProject().getId());
        }

        if (Objects.nonNull(projectDocument.getDocument())) {
            dto.setDocumentId(projectDocument.getDocument().getId());
        }

        projectDocument.getFundingParts().forEach(
                fundingPart -> dto.getFundingParts().add(FundingPartConverter.toDTO(fundingPart)));
        dto.setTextualDescription(MultilingualContentConverter.getMultilingualContentDTO(
                projectDocument.getTextualDescription()));
        dto.setRelationType(projectDocument.getRelationType());

        return dto;
    }

}
