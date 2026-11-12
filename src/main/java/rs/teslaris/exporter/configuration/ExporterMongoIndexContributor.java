package rs.teslaris.exporter.configuration;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;
import rs.teslaris.core.configuration.MongoIndexContributor;
import rs.teslaris.exporter.model.push.PushedEntityState;
import rs.teslaris.exporter.util.ResumptionTokenStash;

@Component
public class ExporterMongoIndexContributor implements MongoIndexContributor {

    @Override
    public List<MongoIndex> indexes() {
        return List.of(
            new MongoIndex(ResumptionTokenStash.class,
                new Index().on("expirationTimestamp", Sort.Direction.ASC).expire(0L)),
            new MongoIndex(PushedEntityState.class,
                new Index().on("target", Sort.Direction.ASC)
                    .on("entityType", Sort.Direction.ASC)
                    .on("databaseId", Sort.Direction.ASC)
                    .unique()));
    }
}
