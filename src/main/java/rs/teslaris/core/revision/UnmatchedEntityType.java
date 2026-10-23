package rs.teslaris.core.revision;

/**
 * The {@code entityType()} of the fallback beans that keep the quality SPI collections non-empty.
 * <p>
 * Spring treats a {@code List<T>} dependency as requiring at least one {@code T} and fails to
 * start when none exists, so {@code core} always contributes one bean per quality SPI. The value
 * is deliberately not a legal {@code EntityType} name, so those beans are never selected for a
 * real record.
 */
final class UnmatchedEntityType {

    static final String VALUE = "__unmatched__";

    private UnmatchedEntityType() {
    }
}
