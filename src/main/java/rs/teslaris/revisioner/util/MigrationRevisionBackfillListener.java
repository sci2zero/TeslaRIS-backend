package rs.teslaris.revisioner.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import rs.teslaris.core.applicationevent.MigrationRunFinishedEvent;
import rs.teslaris.revisioner.service.interfaces.RevisionService;

/**
 * Records a revision of a new major line for every entity a migration run updated, which also
 * carries its reassessment.
 * <p>
 * Entities a run created are not backfilled here: a later pass can still add to them (a person's
 * educations come with the documents), so their baseline revision is left to a backfill run after
 * all passes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MigrationRevisionBackfillListener {

    private final RevisionService revisionService;


    @Async
    @EventListener
    public void handle(MigrationRunFinishedEvent event) {
        if (event.hasUpdatedEntities()) {
            recordUpdated(event);
        }
    }

    private void recordUpdated(MigrationRunFinishedEvent event) {
        var tally = applyToEnumerated(event, event.updatedEntityIdsByType());

        log.info("Recorded {} revision(s) of the entities updated by migration run {} of " +
                "source '{}', {} unchanged, {} failed.", tally.recorded(), event.runId(),
            event.source(), tally.skipped(), tally.failed());
    }

    private Tally applyToEnumerated(MigrationRunFinishedEvent event,
                                    Map<String, List<Integer>> entityIdsByType) {
        var recorded = 0;
        var skipped = 0;
        var failed = 0;

        for (var entry : entityIdsByType.entrySet()) {
            var entityType = entry.getKey();

            for (var entityId : entry.getValue()) {
                if (Objects.isNull(entityId)) {
                    continue;
                }

                try {
                    // Entity types without a revision restorer, and entities whose state is
                    // already recorded, are reported as not recorded rather than as a failure.
                    if (revisionService.recordRevisionFromCurrentState(entityType, entityId,
                        null)) {
                        recorded++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    // One unreadable entity must not cost the rest of the run its history.
                    failed++;
                    log.warn("Unable to record a revision of entity '{}' (ID={}) updated by " +
                            "migration run {}. Reason: {}", entityType, entityId,
                        event.runId(), e.getMessage());
                }
            }
        }

        return new Tally(recorded, skipped, failed);
    }

    private record Tally(int recorded, int skipped, int failed) {
    }
}
