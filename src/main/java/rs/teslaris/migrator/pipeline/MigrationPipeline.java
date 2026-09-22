package rs.teslaris.migrator.pipeline;

import java.util.function.Function;
import rs.teslaris.migrator.client.SourceRecordFetcher;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * Everything needed to migrate one entity type from one source.
 * <p>
 * Registering a new source, or overriding a single entity type with its own endpoint, means
 * declaring one of these as a bean. The runner, registry, retry, logging and reporting are untouched.
 */
public record MigrationPipeline<S>(
    String sourceName,
    MigrationEntityType entityType,
    Class<S> recordClass,
    SourceRecordFetcher<S> fetcher,
    ItemRouter<S> router,
    RetryPolicy retryPolicy,
    Function<S, String> recordIdExtractor,
    int defaultBatchSize
) {

    public String describe() {
        return sourceName + "/" + entityType;
    }
}
