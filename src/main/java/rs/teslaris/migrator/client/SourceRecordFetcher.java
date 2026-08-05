package rs.teslaris.migrator.client;

/**
 * Reads one batch of raw records from a source.
 * <p>
 * The response shape of the source (Spring {@code Page}, plain list, id-then-detail, keyset) is
 * entirely an implementation concern - the runner never sees it. This is also the seam that would
 * let an intermediate staging service be introduced later as just another fetcher.
 */
public interface SourceRecordFetcher<S> {

    SourceBatch<S> fetch(MigrationCursor cursor, int batchSize);
}
