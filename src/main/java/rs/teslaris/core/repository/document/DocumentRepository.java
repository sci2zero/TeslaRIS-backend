package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface DocumentRepository extends JPASoftDeleteRepository<Document> {
}
