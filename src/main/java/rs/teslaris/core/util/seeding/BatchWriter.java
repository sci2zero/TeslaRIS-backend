package rs.teslaris.core.util.seeding;

/**
 * Lets {@link CsvDataLoader} flush whatever a caller has buffered at batch boundaries, without
 * core knowing what is being written. Implementations live with the data they buffer.
 */
public interface BatchWriter {

    void flushBatch();
}
