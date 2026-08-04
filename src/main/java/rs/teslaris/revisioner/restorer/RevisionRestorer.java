package rs.teslaris.revisioner.restorer;

/**
 * Applies a deserialized historical DTO back onto the live entity.
 * <p>
 * Implementations delegate to the entity's regular update method, so the restore goes through the
 * same validation, indexing and revision-creation path as any other edit - the restored state is
 * therefore recorded as a new revision with an incremented minor version.
 */
public interface RevisionRestorer<T> {

    String entityType();

    Class<T> dtoClass();

    void restore(Integer entityId, T dto);
}
