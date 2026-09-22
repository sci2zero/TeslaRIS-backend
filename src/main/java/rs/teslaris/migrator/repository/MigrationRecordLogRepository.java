package rs.teslaris.migrator.repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
