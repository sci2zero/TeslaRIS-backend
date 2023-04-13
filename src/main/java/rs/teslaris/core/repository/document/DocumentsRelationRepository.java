package rs.teslaris.core.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.teslaris.core.model.document.DocumentsRelation;

public interface DocumentsRelationRepository extends JpaRepository<DocumentsRelation, Integer> {
}
