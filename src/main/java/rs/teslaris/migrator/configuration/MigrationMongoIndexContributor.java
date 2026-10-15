package rs.teslaris.migrator.configuration;

import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.stereotype.Component;
import rs.teslaris.core.configuration.MongoIndexContributor;
import rs.teslaris.migrator.model.MigrationRecordLog;

@Component
public class MigrationMongoIndexContributor implements MongoIndexContributor {

    @Override
    public List<MongoIndex> indexes() {
        return List.of(
            new MongoIndex(MigrationRecordLog.class,
                new CompoundIndexDefinition(
                    new Document("source", 1).append("entity_type", 1).append("source_key", 1))
                    .named("source_type_key_idx")
                    .unique()),
            new MongoIndex(MigrationRecordLog.class,
                new CompoundIndexDefinition(new Document("run_id", 1).append("status", 1))
                    .named("run_status_idx")));
    }
}
