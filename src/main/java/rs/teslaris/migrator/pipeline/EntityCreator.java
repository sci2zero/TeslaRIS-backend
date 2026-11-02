package rs.teslaris.migrator.pipeline;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import rs.teslaris.migrator.model.MigrationRun;

/**
 * Creates one entity in the core services and reports the id it received.
 * <p>
 * Every core create method has the shape {@code X create(DTO dto, Boolean index)}, so most creators
 * are built with {@link #of(BiFunction, Function)} from a plain method reference. The returned id is
 * written to the record log, which is what lets dependent entities (employments, contributions)
 * resolve their references later. The run is passed for its {@code performIndex} flag and for
 * creators that record entries of their own (a project's consortium contributions).
 */
@FunctionalInterface
public interface EntityCreator<D> {

    static <D, R> EntityCreator<D> of(BiFunction<D, Boolean, R> createMethod,
                                      Function<R, Integer> idExtractor) {
        return (dto, run) -> {
            var created = createMethod.apply(dto, run.isPerformIndex());
            return Objects.isNull(created) ? null : idExtractor.apply(created);
        };
    }

    Integer create(D dto, MigrationRun run);
}
