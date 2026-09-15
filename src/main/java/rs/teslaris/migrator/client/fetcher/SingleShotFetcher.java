package rs.teslaris.migrator.client.fetcher;

import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import rs.teslaris.migrator.client.MigrationCursor;
import rs.teslaris.migrator.client.SourceBatch;
import rs.teslaris.migrator.client.SourceRecordFetcher;

/**
 * Fetcher for endpoints that return the whole collection as a plain list, with no paging.
 * Everything arrives in the first batch.
 */
@RequiredArgsConstructor
public class SingleShotFetcher<S> implements SourceRecordFetcher<S> {

    private final Supplier<List<S>> call;


    @Override
    public SourceBatch<S> fetch(MigrationCursor cursor, int batchSize) {
        if (cursor.page() > 0) {
            return SourceBatch.empty(cursor);
        }

        return new SourceBatch<>(call.get(), cursor.nextPage(), false);
    }
}
