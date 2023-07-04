package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface PatentRepository extends JPASoftDeleteRepository<Patent> {
}
