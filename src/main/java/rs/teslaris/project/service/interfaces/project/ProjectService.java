package rs.teslaris.project.service.interfaces.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.dto.project.ProjectsRelationDTO;
import rs.teslaris.project.indexmodel.project.ProjectIndex;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.model.project.ProjectStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public interface ProjectService extends JPAService<Project> {

    Page<ProjectIndex> searchProjects(List<String> tokens,
                                      LocalDate dateFrom,
                                      LocalDate dateTo,
                                      boolean onlyActive,
                                      List<ProjectStatus> allowedStatuses,
                                      Pageable pageable);

    ProjectDTO readProject(Integer projectId);

    Project createProject(ProjectDTO projectDTO);

    void updateProject(Integer projectId, ProjectDTO projectDTO);

    void deleteProject(Integer projectId);

    PersonProjectContributionDTO addPerson(Integer projectId, PersonProjectContributionDTO personDto);

    void removePerson(Integer projectId, Integer personId);

    OrganisationUnitProjectContributionDTO addOrganisation(Integer projectId,
                                                          OrganisationUnitProjectContributionDTO organisationDto);

    void removeOrganisation(Integer projectId, Integer organisationId);

    ProjectsRelationDTO addProjectRelation(Integer projectId, ProjectsRelationDTO relationDto);

    void removeProjectRelation(Integer projectId, Integer relationId);

    CompletableFuture<Void> reindexProject();

    void indexProject(Project project, ProjectIndex index);
}
