package rs.teslaris.project.controller.project;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.dto.project.ProjectsRelationDTO;
import rs.teslaris.project.indexmodel.project.ProjectIndex;
import rs.teslaris.project.model.project.ProjectStatus;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/{projectId}/can-edit")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    public boolean canEditProject() {
        return true;
    }

    @GetMapping("/search")
    public Page<ProjectIndex> searchProjects(@RequestParam List<String> tokens,
                                             @RequestParam(required = false)
                                             LocalDate dateFrom,
                                             @RequestParam(required = false)
                                             LocalDate dateTo,
                                             @RequestParam(required = false)
                                             boolean onlyActive,
                                             @RequestParam(required = false)
                                             boolean onlyWithoutContributions,
                                             @RequestParam(required = false)
                                             List<ProjectStatus> allowedStatuses,
                                             Pageable pageable) {
        return projectService.searchProjects(tokens, dateFrom, dateTo, onlyActive, onlyWithoutContributions, allowedStatuses, pageable);
    }

    @GetMapping("/count")
    public Long countAll() {
        return projectService.getProjectCount();
    }

    @GetMapping("/{projectId}")
    public ProjectDTO readProject(@PathVariable Integer projectId) {
        return projectService.readProject(projectId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public ProjectDTO createProject(
        @RequestBody @Valid ProjectDTO projectDTO) {
        var savedProject = projectService.createProject(projectDTO);
        projectDTO.setId(savedProject.getId());

        return projectDTO;
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProject(@PathVariable Integer projectId,
                              @RequestBody @Valid ProjectDTO projectDTO) {
        projectService.updateProject(projectId, projectDTO);
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProjects(@PathVariable Integer projectId) {
        projectService.deleteProject(projectId);
    }


    @PostMapping("/{projectId}/add-person")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public PersonProjectContributionDTO addProjectPerson(
            @PathVariable Integer projectId,
            @RequestBody @Valid PersonProjectContributionDTO personContribution) {
        return projectService.addPerson(projectId, personContribution);
    }

    @DeleteMapping("/{projectId}/remove-person/{personContributionId}")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProjectPerson(@PathVariable Integer projectId,
                                    @PathVariable Integer personContributionId) {
        projectService.removePerson(projectId, personContributionId);
    }

    @PostMapping("/{projectId}/add-organisation")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public OrganisationUnitProjectContributionDTO addProjectOrganisation(
            @PathVariable Integer projectId,
            @RequestBody @Valid OrganisationUnitProjectContributionDTO organisationContribution) {
        return projectService.addOrganisation(projectId, organisationContribution);
    }

    @DeleteMapping("/{projectId}/remove-organisation/{organisationContributionId}")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProjectOrganisation(
            @PathVariable Integer projectId,
            @PathVariable Integer organisationContributionId) {
        projectService.removeOrganisation(projectId, organisationContributionId);
    }

    @PostMapping("/{projectId}/add-relation")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public ProjectsRelationDTO addProjectRelation(
            @PathVariable Integer projectId,
            @RequestBody @Valid ProjectsRelationDTO relation) {
        return projectService.addProjectRelation(projectId, relation);
    }

    @DeleteMapping("/{projectId}/remove-relation/{relationId}")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProjectRelation(@PathVariable Integer projectId,
                                      @PathVariable Integer relationId) {
        projectService.removeProjectRelation(projectId, relationId);
    }
}
