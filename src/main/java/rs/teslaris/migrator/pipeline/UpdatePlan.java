package rs.teslaris.migrator.pipeline;

import java.util.Objects;
import java.util.Optional;

/**
 * How an item of this type is written a second time, when the source record changed.
 * <p>
 * An item without a plan keeps the behaviour of a create only migration: the runner skips it once
 * the record log holds it. A plan without a reader merges in the updater itself, which is what a
 * DTO that wraps the core DTO needs.
 */
public record UpdatePlan<D>(
    ExistingReader<D> reader,
    EntityUpdater<D> updater,
    ExistingTargetLocator<D> locator
) {

    public static <D> UpdatePlan<D> none() {
        return new UpdatePlan<>(null, null, null);
    }

    public static <D> UpdatePlan<D> of(EntityUpdater<D> updater) {
        return new UpdatePlan<>(null, updater, null);
    }

    public static <D> UpdatePlan<D> of(ExistingReader<D> reader, EntityUpdater<D> updater) {
        return new UpdatePlan<>(reader, updater, null);
    }

    public static <D> UpdatePlan<D> of(ExistingReader<D> reader, EntityUpdater<D> updater,
                                       ExistingTargetLocator<D> locator) {
        return new UpdatePlan<>(reader, updater, locator);
    }

    public boolean canUpdate() {
        return Objects.nonNull(updater);
    }

    public boolean hasReader() {
        return Objects.nonNull(reader);
    }

    public Optional<Integer> locate(D dto) {
        return Objects.isNull(locator) ? Optional.empty() : locator.locate(dto);
    }
}
