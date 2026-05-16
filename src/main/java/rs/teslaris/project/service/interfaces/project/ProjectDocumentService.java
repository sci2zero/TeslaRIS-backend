package rs.teslaris.project.service.interfaces.project;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.model.project.ProjectDocument;

@Service
public interface ProjectDocumentService extends JPAService<ProjectDocument> {

    ProjectDocument createProjectDocument(ProjectDocumentDTO projectDocumentDTO);


    @Transactional
    void deleteProjectDocument(Integer projectDocumentId);
}
