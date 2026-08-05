package rs.teslaris.migrator.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * Router for a homogeneous entity type: extract DTOs of one kind, create them all the same way.
 *
 * @param sourceKey identity of each produced item. It lives here rather than in the runner because
 *                  it differs per case - a record id for projects, {@code curriculumId#outputId} for
 *                  outputs, a normalised name where the source provides no id at all.
 */
public record SimpleMapping<S, D>(
    MigrationEntityType type,
    RecordExtractor<S, D> extractor,
    EntityCreator<D> creator,
    FailureHandler<D> failureHandler,
    BiFunction<S, D, String> sourceKey
) implements ItemRouter<S> {

    @Override
    public List<MigrationItem<?>> route(S record) {
        List<MigrationItem<?>> items = new ArrayList<>();

        extractor.extract(record).forEach(dto -> items.add(new MigrationItem<>(
            type, sourceKey.apply(record, dto), dto, creator, failureHandler)));

        return items;
    }
}
