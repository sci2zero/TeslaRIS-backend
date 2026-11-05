package rs.teslaris.migrator.pipeline;

import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * One creatable unit produced from a source record: the DTO, where it goes, and how failures are
 * handled.
 *
 * @param sourceKey stable identity of this item in the source. Used for the record log, which
 *                  provides both resume ("already created, skip") and deduplication (the same
 *                  institution or co-authored paper appears in many curricula).
 * @param legacyKey the key an earlier version of the migration produced for the same item, or
 *                  {@code null}. A run adopts what it finds under this key, so a key format that
 *                  changed does not create a second entity.
 */
public record MigrationItem<D>(
    MigrationEntityType type,
    String sourceKey,
    String legacyKey,
    D dto,
    EntityCreator<D> creator,
    UpdatePlan<D> updatePlan,
    FailureHandler<D> failureHandler
) {

    public MigrationItem(MigrationEntityType type, String sourceKey, D dto,
                         EntityCreator<D> creator, FailureHandler<D> failureHandler) {
        this(type, sourceKey, null, dto, creator, UpdatePlan.none(), failureHandler);
    }

    public Integer create(MigrationRun run) {
        return creator.create(dto, run);
    }

    public boolean canUpdate() {
        return updatePlan.canUpdate();
    }

    public boolean hasReader() {
        return updatePlan.hasReader();
    }

    public D readExisting(Integer targetId) {
        return updatePlan.reader().read(targetId);
    }

    public void update(Integer targetId, D merged, MigrationRun run) {
        updatePlan.updater().update(targetId, merged, run);
    }

    public FailureResolution handleFailure(Exception exception, int attempt) {
        return failureHandler.onCreateFailed(this, exception, attempt);
    }
}
