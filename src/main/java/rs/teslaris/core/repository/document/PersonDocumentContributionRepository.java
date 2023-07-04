package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface PersonDocumentContributionRepository
    extends JPASoftDeleteRepository<PersonDocumentContribution> {
}
