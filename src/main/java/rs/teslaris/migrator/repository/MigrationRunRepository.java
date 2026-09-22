package rs.teslaris.migrator.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.migrator.model.MigrationRun;
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

    public List<MigrationRun> findAll(String source, MigrationEntityType entityType,
                                      Pageable pageable) {
        var query = new Query().with(pageable)
            .with(Sort.by(Sort.Direction.DESC, "started_at"));

        if (Objects.nonNull(source)) {
            query.addCriteria(Criteria.where("source").is(source));
        }

        if (Objects.nonNull(entityType)) {
            query.addCriteria(Criteria.where("entity_type").is(entityType.name()));
        }

        return mongoTemplate.find(query, MigrationRun.class);
    }
}
