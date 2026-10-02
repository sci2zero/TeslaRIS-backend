package rs.teslaris.migrator.pipeline;

/**
 * An in-memory lookup cache that is valid only for one migration run.
 * <p>
 * Resolvers cache misses too, so a cache surviving into the next run would hide entities that an
 * earlier pass created in the meantime. Every bean implementing this is cleared before a run starts.
 */
public interface RunScopedCache {

    void clearCache();
}
