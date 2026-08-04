package rs.teslaris.revisioner.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.revisioner.model.EntityRevision;

@Repository
public interface EntityRevisionRepository extends JpaRepository<EntityRevision, Long> {

    List<EntityRevision>
    findByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(String entityType, Integer entityId);

    Optional<EntityRevision>
    findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(String entityType, Integer entityId);

    Optional<EntityRevision>
    findTopByEntityTypeAndEntityIdAndRevisionTimestampLessThanEqualOrderByRevisionTimestampDesc(
        String entityType, Integer entityId, Instant timestamp);

    Optional<EntityRevision> findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
        String entityType, Integer entityId);

    Optional<EntityRevision>
    findFirstByEntityTypeAndEntityIdAndMajorVersionAndMinorVersionOrderByRevisionTimestampDesc(
        String entityType, Integer entityId, Integer majorVersion, Integer minorVersion);
}
