package rs.teslaris.migrator.pipeline;

import java.util.Optional;

/**
 * Finds the entity of an item whose source key the record log does not hold.
 * <p>
 * Used where the source key is not stable: the migration adopts the entity it finds instead of
 * creating a second one for the same source record.
 */
@FunctionalInterface
public interface ExistingTargetLocator<D> {

    Optional<Integer> locate(D dto);
}
