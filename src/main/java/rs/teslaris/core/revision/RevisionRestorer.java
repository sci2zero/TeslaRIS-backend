package rs.teslaris.core.revision;

/**
 * Applies a deserialized historical DTO back onto the live entity.
 * <p>
 * Implementations delegate to the entity's regular update method, so the restore goes through the
 * same validation, indexing and revision-creation path as any other edit and the restored state is
 * therefore recorded as a new revision with an incremented minor version (if major version of the
 * latest and restored match) or incremented major and minor version (otherwise).
 */
public interface RevisionRestorer<T> {

    String entityType();

    Class<T> dtoClass();

    void restore(Integer entityId, T dto);

    /**
     * The class a stored revision is deserialized into. It is {@link #dtoClass()} wherever the read
     * and write DTOs are the same type, and is overridden where they are siblings rather than a
     * hierarchy - a journal is captured as {@code JournalResponseDTO} but updated from
     * {@code JournalDTO}.
     */
    default Class<?> readDtoClass() {
        return dtoClass();
    }

    /**
     * Reads the entity back after a restore, so the recorded revision describes what the entity
     * actually became rather than what was asked for.
     * <p>
     * These can differ: references that no longer exist are dropped or degraded during a restore.
     * Implementations that return {@code null} fall back to storing the historical payload verbatim.
     *
     * @return the current state of the entity as the DTO its revisions are captured from
     */
    default Object readCurrentState(Integer entityId) {
        return null;
    }
}
