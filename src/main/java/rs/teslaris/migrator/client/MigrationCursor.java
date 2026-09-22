package rs.teslaris.migrator.client;

/**
 * Position in the source traversal.
 * <p>
 * Only {@code page} is used today, since the hydrator exposes page/size paging. {@code afterId} is
 * carried so that a keyset-paginated endpoint can be supported without touching the runner.
 */
public record MigrationCursor(
    int page,
    String afterId
) {

    public static MigrationCursor start() {
        return new MigrationCursor(0, null);
    }

    public MigrationCursor nextPage() {
        return new MigrationCursor(page + 1, afterId);
    }

    public MigrationCursor withAfterId(String newAfterId) {
        return new MigrationCursor(page, newAfterId);
    }
}
