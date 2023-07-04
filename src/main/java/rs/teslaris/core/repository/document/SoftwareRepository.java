package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface SoftwareRepository extends JPASoftDeleteRepository<Software> {
}
