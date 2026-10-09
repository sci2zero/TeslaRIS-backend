package rs.teslaris.project.revision;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.project.ProjectQualityViewDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.revision.RevisionRestorer;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.service.interfaces.project.ProjectService;

/**
 * {@code updateProject} writes only the project's own fields - team, consortium and relations are
 * built when the project is created and managed through their own endpoints afterwards, so they are
 * excluded from the snapshot rather than silently ignored by a restore.
 */
@Component
@RequiredArgsConstructor
public class ProjectRevisionRestorer implements RevisionRestorer<ProjectDTO> {

    private final ProjectService projectService;


    @Override
    public String entityType() {
        return EntityType.PROJECT.name();
    }

    @Override
    public Class<ProjectDTO> dtoClass() {
        return ProjectDTO.class;
    }

    /**
     * The calculator lives in {@code revisioner}, which may not depend on this module, so the
     * assessment reads a core-owned projection of the snapshot rather than ProjectDTO itself.
     */
    @Override
    public Class<?> assessmentDtoClass() {
        return ProjectQualityViewDTO.class;
    }

    @Override
    public void restore(Integer entityId, ProjectDTO dto) {
        projectService.updateProject(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return projectService.readProject(entityId);
    }
}
