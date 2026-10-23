package rs.teslaris.revisioner.util;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import rs.teslaris.core.applicationevent.MigrationRunFinishedEvent;
import rs.teslaris.revisioner.model.QualityAssessmentTarget;
import rs.teslaris.revisioner.service.interfaces.QualityAssessmentBackfillService;
import rs.teslaris.revisioner.service.interfaces.RevisionService;

/**
 * Gives the entities a migration produced the baseline revision and quality assessment it skipped
 * while running.
 * <p>
 * A run that enumerated what it created is backfilled exactly; one that did not is backfilled per
 * type over the whole repository, which skips whatever already has a revision and so also catches
 * entities an earlier run left behind.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MigrationRevisionBackfillListener {

    private final RevisionService revisionService;

    private final QualityAssessmentBackfillService qualityAssessmentBackfillService;


    @Async
    @EventListener
    public void handle(MigrationRunFinishedEvent event) {
        if (event.hasEnumeratedEntities()) {
            backfillEnumerated(event);
            return;
        }

        backfillEveryEntityOfCreatedTypes(event);
    }

    private void backfillEnumerated(MigrationRunFinishedEvent event) {
        var recorded = 0;
        var skipped = 0;
        var failed = 0;

        for (var entry : event.createdEntityIdsByType().entrySet()) {
            var entityType = entry.getKey();

            for (var entityId : entry.getValue()) {
                if (Objects.isNull(entityId)) {
                    continue;
                }

                try {
                    // Entity types without a revision restorer, and entities that already have a
                    // revision, are reported as not recorded rather than as a failure.
                    if (revisionService.createRevisionFromCurrentState(entityType, entityId,
                        null)) {
                        recorded++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    // One unreadable entity must not cost the rest of the run its history.
                    failed++;
                    log.warn("Unable to backfill a revision of entity '{}' (ID={}) created by " +
                            "migration run {}. Reason: {}", entityType, entityId, event.runId(),
                        e.getMessage());
                }
            }
        }

        log.info("Backfilled {} revision(s) of the entities created by migration run {} of " +
                "source '{}', {} skipped, {} failed.", recorded, event.runId(), event.source(),
            skipped, failed);
    }

    private void backfillEveryEntityOfCreatedTypes(MigrationRunFinishedEvent event) {
        var targets = resolveTargets(event.createdEntityTypes());

        if (targets.isEmpty()) {
            log.info("Migration run {} of source '{}' created only types without a backfill " +
                    "target ({}), nothing to backfill.", event.runId(), event.source(),
                event.createdEntityTypes());
            return;
        }

        log.info("Backfilling revisions and quality assessments for {} after migration run {} " +
            "of source '{}'.", targets, event.runId(), event.source());

        // Existing assessments are left alone: this is about the records that have none.
        qualityAssessmentBackfillService.performBackfill(targets, List.of(), List.of(), null,
            false);
    }

    /**
     * Maps the migrator's entity types onto backfill targets. Document subtypes all share the
     * single DOCUMENT target, and types without one - a person employment, for instance, which is
     * part of a person rather than an entity of its own - are dropped.
     */
    private List<QualityAssessmentTarget> resolveTargets(Set<String> createdEntityTypes) {
        return createdEntityTypes.stream()
            .map(this::toTarget)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private QualityAssessmentTarget toTarget(String migrationEntityType) {
        return switch (migrationEntityType) {
            case "PERSON" -> QualityAssessmentTarget.PERSON;
            case "ORGANISATION_UNIT" -> QualityAssessmentTarget.ORGANISATION_UNIT;
            case "DOCUMENT", "JOURNAL_PUBLICATION", "PROCEEDINGS_PUBLICATION", "THESIS",
                 "MONOGRAPH", "MONOGRAPH_PUBLICATION" -> QualityAssessmentTarget.DOCUMENT;
            default -> null;
        };
    }
}
