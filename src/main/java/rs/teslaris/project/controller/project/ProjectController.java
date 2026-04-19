package rs.teslaris.project.controller.project;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}
