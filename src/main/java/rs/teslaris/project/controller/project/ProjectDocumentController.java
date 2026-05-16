package rs.teslaris.project.controller.project;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.service.interfaces.project.ProjectDocumentService;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectDocumentController {

    private final ProjectDocumentService projectDocumentService;

    @PostMapping("/add-document")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public ProjectDocumentDTO addProjectDocument(@RequestBody @Valid
                                                 ProjectDocumentDTO projectDocument) {
        var savedDocument = projectDocumentService.createProjectDocument(projectDocument);
        projectDocument.setId(savedDocument.getId());

        return projectDocument;
    }

    @DeleteMapping("/remove-document/{projectDocumentId}")
    @PreAuthorize("hasAuthority('EDIT_PROJECTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProjectDocument(@PathVariable Integer projectDocumentId) {
        projectDocumentService.deleteProjectDocument(projectDocumentId);
    }

}
