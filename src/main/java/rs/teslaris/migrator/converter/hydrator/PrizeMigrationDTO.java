package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.core.dto.person.PrizeDTO;

/**
 * A prize is added to a person migrated earlier in the same pass; the person is resolved against
 * the record log at creation time. {@code sourceId} is the distinction id, unique only within its
 * curriculum. A non-null {@code rejection} means a source value was invalid; the creator fails the
 * item with it instead of creating the prize.
 */
public record PrizeMigrationDTO(
    String personSourceKey,
    String sourceId,
    PrizeDTO prize,
    String rejection
) {
}
