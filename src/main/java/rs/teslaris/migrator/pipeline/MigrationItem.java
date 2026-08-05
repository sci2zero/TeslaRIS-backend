package rs.teslaris.migrator.pipeline;

import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * One creatable unit produced from a source record: the DTO, where it goes, and how failures are
 * handled.
 *
 * @param sourceKey stable identity of this item in the source. Used for the record log, which
 *                  provides both resume ("already created, skip") and deduplication (the same
 *                  institution or co-authored paper appears in many curricula).
 */
public record MigrationItem<D>(
    MigrationEntityType type,
    String sourceKey,
    D dto,
    EntityCreator<D> creator,
    FailureHandler<D> failureHandler
) {

    public Integer create(boolean performIndex) {
        return creator.create(dto, performIndex);
    }

    public FailureResolution handleFailure(Exception exception, int attempt) {
        return failureHandler.onCreateFailed(this, exception, attempt);
    }
}
