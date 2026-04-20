package rs.teslaris.project.controller.project;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

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
}
