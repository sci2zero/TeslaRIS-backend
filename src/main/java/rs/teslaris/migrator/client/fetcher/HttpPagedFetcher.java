package rs.teslaris.migrator.client.fetcher;

import lombok.RequiredArgsConstructor;
import rs.teslaris.migrator.client.MigrationCursor;
import rs.teslaris.migrator.client.RestPage;
import rs.teslaris.migrator.client.SourceBatch;
import rs.teslaris.migrator.client.SourceRecordFetcher;
import rs.teslaris.migrator.model.MigrationRun;

/**
 * Fetcher for endpoints returning a Spring {@code Page} with page/size parameters.
 */
@RequiredArgsConstructor
public class HttpPagedFetcher<S> implements SourceRecordFetcher<S> {

    private final PageCall<S> pageCall;


    @Override
    public SourceBatch<S> fetch(MigrationCursor cursor, int batchSize, MigrationRun run) {
        var page = pageCall.fetch(run.getModifiedAfter(), cursor.page(), batchSize);

        if (page.content().isEmpty()) {
            return SourceBatch.empty(cursor.nextPage());
        }

        return new SourceBatch<>(page.content(), cursor.nextPage(), !page.last());
    }

    @FunctionalInterface
    public interface PageCall<S> {
        RestPage<S> fetch(String modifiedAfter, int page, int size);
    }
}
