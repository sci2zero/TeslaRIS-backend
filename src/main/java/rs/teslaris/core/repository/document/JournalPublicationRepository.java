package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface JournalPublicationRepository extends JPASoftDeleteRepository<JournalPublication> {
}
