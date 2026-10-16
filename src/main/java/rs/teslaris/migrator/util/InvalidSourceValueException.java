package rs.teslaris.migrator.util;

/**
 * A source value is present but cannot be converted without guessing (e.g. February 30th). The
 * item carrying it fails, so the source can be corrected and the item retried.
 */
public class InvalidSourceValueException extends MigrationException {

    public InvalidSourceValueException(String message) {
        super(message);
    }
}
