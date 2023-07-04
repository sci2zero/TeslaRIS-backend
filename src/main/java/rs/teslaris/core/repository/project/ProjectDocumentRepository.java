package rs.teslaris.core.repository.project;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.ProjectDocument;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface ProjectDocumentRepository extends JPASoftDeleteRepository<ProjectDocument> {
}
