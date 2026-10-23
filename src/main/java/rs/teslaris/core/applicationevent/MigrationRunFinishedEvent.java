package rs.teslaris.core.applicationevent;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Raised once a migration run has stopped, successfully or not, naming what it created.
 * <p>
 * A migration skips the per-record bookkeeping that interactive editing performs, so the entities
 * it produced have no revision and no quality assessment yet. This event is the hand-off to the
 * modules that make up for that in bulk, and is deliberately raised outside the migration context
 * so their own bookkeeping is not suppressed in turn.
 *
 * @param createdEntityTypes     entity type names as the migrator knows them, always populated.
 *                               Types a consumer does not recognise are expected to be ignored.
 * @param createdEntityIdsByType the exact entities created, keyed by the same type names, or
 *                               {@code null} when the run asked not to enumerate them. Present
 *                               means "process these"; absent means "process everything of these
 *                               types, skipping whatever is already in order". The choice is the
 *                               operator's, because the two have opposite costs: enumerating is
 *                               precise but proportional to the size of the run, sweeping is
 *                               constant in the size of the run but proportional to the size of
 *                               the repository.
 */
public record MigrationRunFinishedEvent(
    String runId,
    String source,
    Set<String> createdEntityTypes,

    @Nullable
    Map<String, List<Integer>> createdEntityIdsByType
) {

    public boolean hasEnumeratedEntities() {
        return Objects.nonNull(createdEntityIdsByType) &&
            !createdEntityIdsByType.isEmpty();
    }
}
