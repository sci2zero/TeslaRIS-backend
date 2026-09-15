package rs.teslaris.migrator.pipeline;

import java.util.Objects;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * A pipeline together with the entity type the caller actually asked for.
 * <p>
 * When a request for a specific document subtype falls back to the generic document pipeline, the
 * router still emits every subtype it can produce, so the runner filters items down to
 * {@code requestedType}.
 */
public record ResolvedPipeline<S>(
    MigrationPipeline<S> pipeline,
    MigrationEntityType requestedType
) {

    public boolean accepts(MigrationItem<?> item) {
        return Objects.equals(pipeline.entityType(), requestedType) ||
            Objects.equals(item.type(), requestedType);
    }
}
