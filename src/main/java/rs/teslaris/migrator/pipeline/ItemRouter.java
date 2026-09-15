package rs.teslaris.migrator.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns one source record into the items to create from it.
 * <p>
 * This is the only shape the runner consumes. Homogeneous entity types build one from a
 * {@link SimpleMapping}; heterogeneous ones (a curriculum output that may be a journal article, a
 * dissertation, a book chapter, ...) implement it directly.
 */
@FunctionalInterface
public interface ItemRouter<S> {

    /**
     * Runs several routers over the same record, in order. Used by the entities pass, where
     * organisation units must be created before the person that references them, and the person
     * before their employments.
     */
    static <S> ItemRouter<S> ordered(List<ItemRouter<S>> routers) {
        return record -> {
            List<MigrationItem<?>> items = new ArrayList<>();
            routers.forEach(router -> items.addAll(router.route(record)));
            return items;
        };
    }

    List<MigrationItem<?>> route(S record);
}
