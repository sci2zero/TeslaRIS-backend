package rs.teslaris.migrator.pipeline;

import java.util.function.BiConsumer;
import rs.teslaris.migrator.model.MigrationRun;

/**
 * Writes the merged DTO over the entity a previous run created.
 * <p>
 * The counterpart of {@link EntityCreator} for a sync run. Every core edit method has the shape
 * {@code void edit(Integer id, DTO dto)}, so most updaters are built with {@link #of(BiConsumer)}
 * from a plain method reference. The target id is the one the record log holds for the source key.
 */
@FunctionalInterface
public interface EntityUpdater<D> {

    static <D> EntityUpdater<D> of(BiConsumer<Integer, D> editMethod) {
        return (targetId, dto, run) -> editMethod.accept(targetId, dto);
    }

    void update(Integer targetId, D dto, MigrationRun run);
}
