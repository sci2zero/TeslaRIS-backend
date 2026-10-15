package rs.teslaris.core.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
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
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexInitializer {

    private final MongoTemplate mongoTemplate;

    private final ObjectProvider<MongoIndexContributor> indexContributors;


    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        indexContributors.stream()
            .flatMap(contributor -> contributor.indexes().stream())
            .forEach(this::ensureIndex);
    }

    private void ensureIndex(MongoIndexContributor.MongoIndex index) {
        try {
            mongoTemplate.indexOps(index.documentType()).ensureIndex(index.definition());
        } catch (DataAccessException e) {
            log.warn("Could not create Mongo index {} on {}: {}",
                index.definition().getIndexOptions(), index.documentType().getSimpleName(),
                e.getMessage());
        }
    }
}
