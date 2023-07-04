package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface DatasetRepository extends JPASoftDeleteRepository<Dataset> {
}
