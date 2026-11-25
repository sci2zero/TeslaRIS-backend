package rs.teslaris.project.migration;

import java.util.ArrayList;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.migration.ProjectDocumentMigrationWriter;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.model.project.ProjectDocumentType;
import rs.teslaris.project.repository.project.ProjectDocumentRepository;
import rs.teslaris.project.service.interfaces.project.ProjectDocumentService;

@Service
@RequiredArgsConstructor
public class ProjectDocumentMigrationWriterImpl implements ProjectDocumentMigrationWriter {

    private final ProjectDocumentService projectDocumentService;

    private final ProjectDocumentRepository projectDocumentRepository;


    @Override
    @Transactional
    public MigratedProjectDocument linkResult(Integer projectId, Integer documentId) {
        var existing = projectDocumentRepository.findAllByProjectId(projectId).stream()
            .filter(link -> Objects.nonNull(link.getDocument()) &&
                documentId.equals(link.getDocument().getId()))
            .findFirst();

        if (existing.isPresent()) {
            return new MigratedProjectDocument(existing.get().getId(), false);
        }

        var link = new ProjectDocumentDTO();
        link.setProjectId(projectId);
        link.setDocumentId(documentId);
        link.setRelationType(ProjectDocumentType.RESULT);
        link.setTextualDescription(new ArrayList<>());

        var created = projectDocumentService.createProjectDocument(link);

        return new MigratedProjectDocument(created.getId(), true);
    }
}
