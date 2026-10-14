package rs.teslaris.migrator.repository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import rs.teslaris.migrator.model.MigrationItemStatus;
import rs.teslaris.migrator.model.MigrationRecordLog;
import rs.teslaris.migrator.util.MigrationEntityType;

@Repository
@RequiredArgsConstructor
public class MigrationRecordLogRepository {

    private final MongoTemplate mongoTemplate;


    /**
     * Upsert on (source, entityType, sourceKey), so a re-run overwrites the previous outcome for the
     * same item instead of piling up duplicates.
     */
    public void record(MigrationRecordLog entry) {
        var query = keyQuery(entry.getSource(), entry.getEntityType(), entry.getSourceKey());

        var update = new Update()
            .set("run_id", entry.getRunId())
            .set("status", entry.getStatus().name())
            .set("target_entity_id", entry.getTargetEntityId())
            .set("reason", entry.getReason())
            .set("processed_at",
                Objects.requireNonNullElseGet(entry.getProcessedAt(), Instant::now))
            .set("source", entry.getSource())
            .set("entity_type", entry.getEntityType().name())
            .set("source_key", entry.getSourceKey());

        mongoTemplate.upsert(query, update, MigrationRecordLog.class);
    }

    public Optional<MigrationRecordLog> find(String source, MigrationEntityType entityType,
                                             String sourceKey) {
        return Optional.ofNullable(mongoTemplate.findOne(
            keyQuery(source, entityType, sourceKey), MigrationRecordLog.class));
    }

    /**
     * An item counts as processed when it was created or resolved. Failures and skips are retried on
     * the next run.
     */
    public boolean isAlreadyProcessed(String source, MigrationEntityType entityType,
                                      String sourceKey) {
        var query = keyQuery(source, entityType, sourceKey)
            .addCriteria(Criteria.where("status")
                .in(MigrationItemStatus.CREATED.name(), MigrationItemStatus.RESOLVED.name()));

        return mongoTemplate.exists(query, MigrationRecordLog.class);
    }

    public Optional<Integer> findTargetEntityId(String source, MigrationEntityType entityType,
                                                String sourceKey) {
        return find(source, entityType, sourceKey)
            .map(MigrationRecordLog::getTargetEntityId)
            .filter(Objects::nonNull);
    }

    public List<MigrationRecordLog> findFailures(String runId, Pageable pageable) {
        var query = new Query()
            .addCriteria(Criteria.where("run_id").is(runId))
            .addCriteria(Criteria.where("status").is(MigrationItemStatus.FAILED.name()))
            .with(pageable);

        return mongoTemplate.find(query, MigrationRecordLog.class);
    }

    /**
     * Entity types the run actually created something of. Only the types are returned, not the
     * identifiers: the backfill works per type over the whole repository, so a run that created a
     * single person is enough to warrant a person backfill, and the identifiers would be a large
     * payload for no gain. Resolved items are left out, they point at entities that already
     * existed and are therefore already accounted for.
     */
    public Set<String> findCreatedEntityTypes(String runId) {
        var query = new Query()
            .addCriteria(Criteria.where("run_id").is(runId))
            .addCriteria(Criteria.where("status").is(MigrationItemStatus.CREATED.name()));

        return new HashSet<>(
            mongoTemplate.findDistinct(query, "entity_type", MigrationRecordLog.class,
                String.class));
    }

    /**
     * Identifiers of the entities a run created, grouped by entity type. Only worth asking for
     * when the run is small enough that the precise set is cheaper than a sweep of every entity of
     * those types - see {@code backfillOnlyCreated} on the run.
     */
    public Map<String, List<Integer>> findCreatedTargetIdsByType(String runId) {
        var query = new Query()
            .addCriteria(Criteria.where("run_id").is(runId))
            .addCriteria(Criteria.where("status").is(MigrationItemStatus.CREATED.name()))
            .addCriteria(Criteria.where("target_entity_id").ne(null));

        // Only two of the eight fields are of interest, the rest stay in the database.
        query.fields().include("entity_type").include("target_entity_id");

        return mongoTemplate.find(query, MigrationRecordLog.class).stream()
            .filter(entry -> Objects.nonNull(entry.getTargetEntityId()))
            .collect(Collectors.groupingBy(entry -> entry.getEntityType().name(),
                Collectors.mapping(MigrationRecordLog::getTargetEntityId, Collectors.toList())));
    }

    public long deleteFailures(String runId) {
        var query = new Query()
            .addCriteria(Criteria.where("run_id").is(runId))
            .addCriteria(Criteria.where("status").is(MigrationItemStatus.FAILED.name()));

        return mongoTemplate.remove(query, MigrationRecordLog.class).getDeletedCount();
    }

    private Query keyQuery(String source, MigrationEntityType entityType, String sourceKey) {
        return new Query()
            .addCriteria(Criteria.where("source").is(source))
            .addCriteria(Criteria.where("entity_type").is(entityType.name()))
            .addCriteria(Criteria.where("source_key").is(sourceKey));
    }
}
