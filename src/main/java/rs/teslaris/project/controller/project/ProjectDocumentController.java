package rs.teslaris.project.controller.project;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.service.interfaces.project.ProjectDocumentService;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectDocumentController {

    private final ProjectDocumentService projectDocumentService;

    @GetMapping("/{projectId}/documents")
    public List<ProjectDocumentDTO> readProjectDocuments(@PathVariable Integer projectId) {
        return projectDocumentService.readProjectDocuments(projectId);
    }

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
