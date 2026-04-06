package rs.teslaris.project.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.project.model.project.ProjectDocument;
import rs.teslaris.project.repository.project.ProjectDocumentRepository;
import rs.teslaris.project.service.interfaces.project.ProjectDocumentService;

@Service
@RequiredArgsConstructor
public class ProjectDocumentServiceImpl extends JPAServiceImpl<ProjectDocument>
    implements ProjectDocumentService {

    private final ProjectDocumentRepository projectDocumentRepository;

    @Override
    protected JpaRepository<ProjectDocument, Integer> getEntityRepository() {
        return projectDocumentRepository;
    }
}
