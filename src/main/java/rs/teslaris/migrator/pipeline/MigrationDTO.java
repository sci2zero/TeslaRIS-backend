package rs.teslaris.migrator.pipeline;

import java.util.Objects;
import rs.teslaris.migrator.util.InvalidSourceValueException;

/**
 * A DTO whose references are resolved by its creator at creation time, not by the extractor.
 * <p>
 * A non-null {@link #rejection()} means a source value was invalid; the creator fails the item with
 * it instead of creating the entity, so the source can be corrected and the item retried.
 */
public interface MigrationDTO {

    // Identity of the item in the record log
    String sourceKey();

    String rejection();

    default void throwIfRejected() {
        if (Objects.nonNull(rejection())) {
            throw new InvalidSourceValueException(rejection());
        }
    }
}
