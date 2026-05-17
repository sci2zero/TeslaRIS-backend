package rs.teslaris.project.controller.project;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.project.dto.project.ProjectEventDTO;
import rs.teslaris.project.service.interfaces.project.ProjectEventService;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectEventController {

    private final ProjectEventService projectEventService;

    @PostMapping("/add-event")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public ProjectEventDTO addProjectEvent(@RequestBody @Valid
                                                 ProjectEventDTO projectEvent) {
        var savedEvent = projectEventService.createProjectEvent(projectEvent);
        projectEvent.setId(savedEvent.getId());

        return projectEvent;
    }

}
