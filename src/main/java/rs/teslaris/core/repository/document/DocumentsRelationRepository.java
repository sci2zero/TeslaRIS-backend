package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.DocumentsRelation;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface DocumentsRelationRepository extends JPASoftDeleteRepository<DocumentsRelation> {
}
