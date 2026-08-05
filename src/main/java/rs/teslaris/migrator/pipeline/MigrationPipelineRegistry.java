package rs.teslaris.migrator.pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * Holds every registered pipeline and resolves (source, entity type) requests, falling back to the
 * parent entity type when a specific one has no dedicated pipeline.
 */
@Component
@Slf4j
public class MigrationPipelineRegistry {

    private final Map<String, MigrationPipeline<?>> pipelines = new HashMap<>();


    public MigrationPipelineRegistry(List<MigrationPipeline<?>> registeredPipelines) {
        registeredPipelines.forEach(pipeline -> {
            var key = key(pipeline.sourceName(), pipeline.entityType());

            if (pipelines.containsKey(key)) {
                throw new IllegalStateException(
                    "Duplicate migration pipeline registered for " + pipeline.describe() + ".");
            }

            pipelines.put(key, pipeline);
            log.info("Registered migration pipeline {}.", pipeline.describe());
        });
    }

    @SuppressWarnings("unchecked")
    public <S> Optional<ResolvedPipeline<S>> resolve(String source,
                                                     MigrationEntityType entityType) {
        var exactMatch = pipelines.get(key(source, entityType));

        if (Objects.nonNull(exactMatch)) {
            return Optional.of(
                new ResolvedPipeline<>((MigrationPipeline<S>) exactMatch, entityType));
        }

        var parentType = entityType.parent();

        if (Objects.isNull(parentType)) {
            return Optional.empty();
        }

        return Optional.ofNullable(pipelines.get(key(source, parentType)))
            .map(fallback -> new ResolvedPipeline<>((MigrationPipeline<S>) fallback, entityType));
    }

    public List<String> listRegistered() {
        return pipelines.values().stream().map(MigrationPipeline::describe).sorted().toList();
    }

    private String key(String source, MigrationEntityType entityType) {
        return source + "#" + entityType;
    }
}
