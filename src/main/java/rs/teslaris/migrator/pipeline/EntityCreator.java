package rs.teslaris.migrator.pipeline;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Creates one entity in the core services and reports the id it received.
 * <p>
 * Every core create method has the shape {@code X create(DTO dto, Boolean index)}, so most creators
 * are built with {@link #of(BiFunction, Function)} from a plain method reference. The returned id is
 * written to the record log, which is what lets dependent entities (employments, contributions)
 * resolve their references later.
 */
@FunctionalInterface
public interface EntityCreator<D> {

    static <D, R> EntityCreator<D> of(BiFunction<D, Boolean, R> createMethod,
                                      Function<R, Integer> idExtractor) {
        return (dto, performIndex) -> {
            var created = createMethod.apply(dto, performIndex);
            return Objects.isNull(created) ? null : idExtractor.apply(created);
        };
    }

    Integer create(D dto, boolean performIndex);
}
