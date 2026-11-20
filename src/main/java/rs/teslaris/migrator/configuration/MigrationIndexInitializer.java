package rs.teslaris.migrator.configuration;

import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.model.MigrationRecordLog;

/**
 * Creates the record log indexes declared on {@link MigrationRecordLog}.
 * <p>
 * Spring Data MongoDB builds indexes from annotations only with {@code auto-index-creation}, which
 * is off application-wide. Every migrated item looks itself up in the record log, so without the
 * index each lookup scans the whole collection. Only this collection is handled here, so other
 * collections keep their current indexes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MigrationIndexInitializer {

    private final MongoTemplate mongoTemplate;


    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        var resolver = new MongoPersistentEntityIndexResolver(
            mongoTemplate.getConverter().getMappingContext());
        var indexOperations = mongoTemplate.indexOps(MigrationRecordLog.class);

        var indexes = new ArrayList<IndexDefinition>();
        resolver.resolveIndexFor(MigrationRecordLog.class).forEach(indexes::add);

        for (var index : indexes) {
            var name = index.getIndexOptions().get("name");

            try {
                indexOperations.ensureIndex(index);
                log.info("Ensured index '{}' on migration_record_log.", name);
            } catch (DataAccessResourceFailureException e) {
                log.warn("Mongo is not reachable, so {} of {} indexes on migration_record_log "
                        + "were not created. The next startup tries again.",
                    indexes.size() - indexes.indexOf(index), indexes.size());
                return;
            } catch (RuntimeException e) {
                // A unique index cannot be built over duplicate keys; migration still works, slower
                log.error("Could not create index '{}' on migration_record_log, lookups will scan "
                    + "the whole collection. Check it for duplicate keys.", name, e);
            }
        }
    }
}
