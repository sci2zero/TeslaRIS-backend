package rs.teslaris.project.service.interfaces.project;

import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.Project;

// Used for complex Project creation based on prepopulated metadata.
public interface ProjectCreationService {

    Project createProject(ProjectDTO projectDTO);
}
