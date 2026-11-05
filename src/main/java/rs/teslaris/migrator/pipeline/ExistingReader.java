package rs.teslaris.migrator.pipeline;

import java.util.function.Function;

/**
 * Reads the entity a previous run wrote, as the DTO the migration produces for its type.
 * <p>
 * The merge compares the source DTO against this one, so both sides must be of the same shape.
 * Most core services satisfy that already, since their response DTO extends the DTO their create
 * and edit methods take.
 */
@FunctionalInterface
public interface ExistingReader<D> {

    static <D, R extends D> ExistingReader<D> of(Function<Integer, R> readMethod) {
        return readMethod::apply;
    }

    D read(Integer targetId);
}
