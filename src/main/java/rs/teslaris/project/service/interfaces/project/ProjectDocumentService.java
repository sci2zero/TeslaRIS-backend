package rs.teslaris.project.service.interfaces.project;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.model.project.ProjectDocument;

@Service
public interface ProjectDocumentService extends JPAService<ProjectDocument> {

    List<ProjectDocumentDTO> readProjectDocuments(Integer projectId);

    ProjectDocument createProjectDocument(ProjectDocumentDTO projectDocumentDTO);

    void deleteProjectDocument(Integer projectDocumentId);

}
