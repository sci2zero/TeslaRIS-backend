package rs.teslaris.revisioner.service.interfaces;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import rs.teslaris.revisioner.model.RevisionCreateEvent;

public interface RevisionService {

    boolean createRevisionIfChanged(RevisionCreateEvent event);

    List<LocalDateTime> getRevisionJsons(String entityType, Integer entityId);

    <T> Optional<T> getRevisionAtDate(String entityType, Integer entityId, Instant timestamp,
                                      Class<T> dtoClass);
}
