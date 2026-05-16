package rs.teslaris.project.service.interfaces.project;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.model.project.ProjectDocument;

import java.util.concurrent.CompletableFuture;

@Service
public interface ProjectDocumentService extends JPAService<ProjectDocument> {

    ProjectDocument createProjectDocument(ProjectDocumentDTO projectDocumentDTO);

    void deleteProjectDocument(Integer projectDocumentId);

    CompletableFuture<Void> reindexProjectDocuments();
}
