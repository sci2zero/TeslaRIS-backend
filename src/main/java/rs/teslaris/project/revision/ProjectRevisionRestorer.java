package rs.teslaris.project.revision;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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

    @Override
    public void restore(Integer entityId, ProjectDTO dto) {
        projectService.updateProject(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return projectService.readProject(entityId);
    }
}
