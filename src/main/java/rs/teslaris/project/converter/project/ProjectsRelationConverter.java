package rs.teslaris.project.converter.project;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.project.dto.project.ProjectsRelationDTO;
import rs.teslaris.project.model.project.ProjectsRelation;

public class ProjectsRelationConverter {

    public static ProjectsRelationDTO toDTO(ProjectsRelation relation) {
        var dto = new ProjectsRelationDTO();

        dto.setId(relation.getId());
        dto.setRelationType(relation.getRelationType());
        dto.setDateFrom(relation.getDateFrom());
        dto.setDateTo(relation.getDateTo());

        dto.setSourceProjectDescription(
                MultilingualContentConverter.getMultilingualContentDTO(
                        relation.getSourceProjectDescription()));
        dto.setTargetProjectDescription(
                MultilingualContentConverter.getMultilingualContentDTO(
                        relation.getTargetProjectDescription()));

        if (Objects.nonNull(relation.getSourceProject())) {
            dto.setSourceProjectId(relation.getSourceProject().getId());
            dto.setSourceProjectName(
                    MultilingualContentConverter.getMultilingualContentDTO(
                            relation.getSourceProject().getName()));
        }

        if (Objects.nonNull(relation.getTargetProject())) {
            dto.setTargetProjectId(relation.getTargetProject().getId());
            dto.setTargetProjectName(
                    MultilingualContentConverter.getMultilingualContentDTO(
                            relation.getTargetProject().getName()));
        }

        return dto;
    }
}
