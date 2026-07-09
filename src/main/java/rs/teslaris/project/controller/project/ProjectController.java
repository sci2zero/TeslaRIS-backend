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
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.indexmodel.project.ProjectIndex;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('READ_PROJECTS')")
    public Page<ProjectIndex> searchProjects(@RequestParam List<String> tokens,
                                             @RequestParam(required = false)
                                             LocalDate dateFrom,
                                             @RequestParam(required = false)
                                             LocalDate dateTo,
                                             Pageable pageable) {
        return projectService.searchProjects(tokens, dateFrom, dateTo, pageable);
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("hasAuthority('READ_PROJECTS')")
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

}
