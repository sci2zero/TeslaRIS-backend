package rs.teslaris.migrator.converter.hydrator;

public final class HydratorSource {

    public static final String NAME = "hydrator";

    /**
     * Sort key pinned on the client call. See the design document - offset paging over a mutable
     * sort key can skip records, which is why keyset pagination was requested from hydrator.
     */
    public static final String CURRICULA_SORT = "full_name,asc";

    private HydratorSource() {
    }
}
