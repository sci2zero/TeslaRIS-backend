package rs.teslaris.project.service.interfaces.project;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.indexmodel.project.ProjectIndex;
import rs.teslaris.project.model.project.Project;

import java.util.concurrent.CompletableFuture;

@Service
public interface ProjectService extends JPAService<Project> {

    ProjectDTO readProject(Integer projectId);

    Project createProject(ProjectDTO projectDTO);

    void updateProject(Integer projectId, ProjectDTO projectDTO);

    void deleteProject(Integer projectId);

    CompletableFuture<Void> reindexProject();

    void indexProject(Project project, ProjectIndex index);
}
