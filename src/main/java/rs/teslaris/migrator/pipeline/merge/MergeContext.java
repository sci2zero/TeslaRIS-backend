package rs.teslaris.migrator.pipeline.merge;

/**
 * What a merge log line needs to identify the item it describes.
 */
public record MergeContext(
    String source,
    String entity,
    String key
) {
}
