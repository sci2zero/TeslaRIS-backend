package rs.teslaris.revisioner.service.interfaces;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import rs.teslaris.revisioner.dto.RevisionDTO;
import rs.teslaris.revisioner.model.RevisionCreateEvent;

@Service
public interface RevisionService {

    void createRevisionIfChanged(RevisionCreateEvent event);

    boolean createRevisionFromCurrentState(String entityType, Integer entityId);

    List<RevisionDTO> getRevisions(String entityType, Integer entityId);

    Optional<String> getRevisionAtTimestamp(String entityType, Integer entityId, Instant timestamp);

    void restoreRevision(String entityType, Integer entityId, Integer majorVersion,
                         Integer minorVersion);
}
