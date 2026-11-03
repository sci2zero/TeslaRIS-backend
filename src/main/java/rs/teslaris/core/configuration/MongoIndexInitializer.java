package rs.teslaris.core.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates the Mongo indexes declared by {@link MongoIndexContributor} beans.
 * <p>
 * Annotation driven index creation is off ({@code autoIndexCreation()} defaults to false since
 * Spring Data MongoDB 3.0), so the {@code @Indexed} and {@code @CompoundIndex} declarations on the
 * documents are documentation only - the contributors are what actually create them.
 * <p>
 * {@code ensureIndex} is idempotent for an index that already exists with the same options, so this
 * is safe to run on every startup. Failures are logged and never abort startup: an index is a
 * performance and integrity aid here, not something the application needs in order to boot.
 * <p>
 * This runs on the startup thread. When Mongo cannot be reached, each index would wait the full
 * server selection timeout, so the first connection failure skips all remaining indexes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexInitializer {

    private final MongoTemplate mongoTemplate;

    private final ObjectProvider<MongoIndexContributor> indexContributors;


    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        var indexes = indexContributors.stream()
            .flatMap(contributor -> contributor.indexes().stream())
            .toList();

        for (var index : indexes) {
            if (!ensureIndex(index)) {
                log.warn("Mongo is not reachable, so {} of {} Mongo indexes were not created. " +
                    "The next startup tries again.",
                    indexes.size() - indexes.indexOf(index), indexes.size());
                return;
            }
        }
    }

    /**
     * @return false when Mongo could not be reached, true otherwise, also when this one index
     * failed for a different reason.
     */
    private boolean ensureIndex(MongoIndexContributor.MongoIndex index) {
        try {
            mongoTemplate.indexOps(index.documentType()).ensureIndex(index.definition());
        } catch (DataAccessResourceFailureException e) {
            log.warn("Could not reach Mongo to create index {} on {}: {}",
                index.definition().getIndexOptions(), index.documentType().getSimpleName(),
                e.getMessage());
            return false;
        } catch (DataAccessException e) {
            log.warn("Could not create Mongo index {} on {}: {}",
                index.definition().getIndexOptions(), index.documentType().getSimpleName(),
                e.getMessage());
        }

        return true;
    }
}
