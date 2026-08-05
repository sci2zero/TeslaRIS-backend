package rs.teslaris.migrator.client;

import java.util.List;

public record SourceBatch<S>(
    List<S> records,
    MigrationCursor nextCursor,
    boolean hasMore
) {

    public static <S> SourceBatch<S> empty(MigrationCursor cursor) {
        return new SourceBatch<>(List.of(), cursor, false);
    }
}
