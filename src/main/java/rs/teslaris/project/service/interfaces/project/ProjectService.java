package rs.teslaris.project.service.interfaces.project;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.Project;

@Service
public interface ProjectService extends JPAService<Project> {

    ProjectDTO readProject(Integer projectId);

}
