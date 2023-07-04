package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface MonographRepository extends JPASoftDeleteRepository<Monograph> {
}
