package rs.teslaris.core.service.impl.eventlistener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rs.teslaris.core.applicationevent.PublicationCandidacyChangedEvent;
import rs.teslaris.core.revision.PublicationCandidacyUpdater;

@Component
@Slf4j
public class PublicationCandidacyEventListener {

    private final Map<String, PublicationCandidacyUpdater> updaters = new HashMap<>();


    @Autowired
    public PublicationCandidacyEventListener(List<PublicationCandidacyUpdater> updaters) {
        updaters.forEach(updater -> updater.entityTypes().forEach(entityType -> {
            var previous = this.updaters.put(entityType, updater);

            if (Objects.nonNull(previous)) {
                throw new IllegalStateException(
                    "Two publication candidacy updaters registered for " + entityType + ".");
            }
        }));
    }

    @EventListener
    public void handlePublicationCandidacyChanged(PublicationCandidacyChangedEvent event) {
        var updater = updaters.get(event.entityType());

        if (Objects.isNull(updater)) {
            log.debug("No publication candidacy updater for entity type {}.",
                event.entityType());
            return;
        }

        try {
            updater.apply(event.entityType(), event.entityId(), event.publicationCandidate());
        } catch (Exception e) {
            log.error("Failed to apply publication candidacy {} to {} {}.",
                event.publicationCandidate(), event.entityType(), event.entityId(), e);
        }
    }
}
