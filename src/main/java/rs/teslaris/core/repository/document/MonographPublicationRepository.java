package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface MonographPublicationRepository
    extends JPASoftDeleteRepository<MonographPublication> {
}
