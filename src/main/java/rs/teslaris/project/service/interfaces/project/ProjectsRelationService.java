package rs.teslaris.project.service.interfaces.project;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.project.ProjectsRelationDTO;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.model.project.ProjectsRelation;

@Service
public interface ProjectsRelationService extends JPAService<ProjectsRelation> {

    ProjectsRelation createRelation(ProjectsRelationDTO dto, Project project);
}
