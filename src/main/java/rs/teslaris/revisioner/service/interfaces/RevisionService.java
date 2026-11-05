package rs.teslaris.revisioner.service.interfaces;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import rs.teslaris.core.applicationevent.RevisionCreateEvent;
import rs.teslaris.revisioner.dto.RevisionDTO;

@Service
public interface RevisionService {

    void createRevisionIfChanged(RevisionCreateEvent event);

    boolean createRevisionFromCurrentState(String entityType, Integer entityId,
                                           String profileName);

    /**
     * Records the present state as a revision of a new major line, for an entity that a migration
     * run updated. It writes the baseline 1.0 of an entity that has no revision, and nothing at
     * all when the state matches the newest revision.
     */
    boolean recordRevisionFromCurrentState(String entityType, Integer entityId,
                                           String profileName);

    List<RevisionDTO> getRevisions(String entityType, Integer entityId);

    Optional<String> getRevisionAtTimestamp(String entityType, Integer entityId, Instant timestamp);

    void restoreRevision(String entityType, Integer entityId, Integer majorVersion,
                         Integer minorVersion);

    /**
     * Writes an already mutated snapshot DTO back onto the live entity through its
     * {@code RevisionRestorer} and records the result as a new revision stamped with
     * {@code adminNote}.
     */
    void applyFixedState(String entityType, Integer entityId, Object dto, String adminNote);
}
