package rs.teslaris.core.repository.commontypes;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.DocumentDeduplicationBlacklist;

@Repository
public interface DocumentDeduplicationBlacklistRepository
    extends JpaRepository<DocumentDeduplicationBlacklist, Integer> {

    Optional<DocumentDeduplicationBlacklist> findByLeftDocumentIdAndRightDocumentId(
        Integer leftDocumentId, Integer rightDocumentId);
}
