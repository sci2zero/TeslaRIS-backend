package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface AffiliationStatementRepository
    extends JPASoftDeleteRepository<AffiliationStatement> {
}
