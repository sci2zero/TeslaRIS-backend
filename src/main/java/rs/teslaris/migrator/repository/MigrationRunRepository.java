package rs.teslaris.migrator.repository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.MigrationRunStatus;
import rs.teslaris.migrator.util.MigrationEntityType;

@Repository
@RequiredArgsConstructor
public class MigrationRunRepository {

    private final MongoTemplate mongoTemplate;


    public MigrationRun save(MigrationRun run) {
        return mongoTemplate.save(run);
    }

    public Optional<MigrationRun> findById(String runId) {
        return Optional.ofNullable(mongoTemplate.findById(runId, MigrationRun.class));
    }

    public Page<MigrationRun> findAll(String source, MigrationEntityType entityType,
                                      Pageable pageable) {
        var query = runQuery(source, entityType).with(pageable)
            .with(Sort.by(Sort.Direction.DESC, "started_at"));

        // The count is asked over a query of its own, so the page of the listing does not limit it.
        var total = mongoTemplate.count(runQuery(source, entityType), MigrationRun.class);

        return new PageImpl<>(mongoTemplate.find(query, MigrationRun.class), pageable, total);
    }

    /**
     * Marks every run that is still recorded as running as failed, and gives back how many.
     */
    public long failRunningRuns() {
        var query = new Query()
            .addCriteria(Criteria.where("status").is(MigrationRunStatus.RUNNING.name()));

        var update = new Update()
            .set("status", MigrationRunStatus.FAILED.name())
            .set("finished_at", Instant.now());

        return mongoTemplate.updateMulti(query, update, MigrationRun.class).getModifiedCount();
    }

    private Query runQuery(String source, MigrationEntityType entityType) {
        var query = new Query();

        if (Objects.nonNull(source)) {
            query.addCriteria(Criteria.where("source").is(source));
        }

        if (Objects.nonNull(entityType)) {
            query.addCriteria(Criteria.where("entity_type").is(entityType.name()));
        }

        return query;
    }
}
