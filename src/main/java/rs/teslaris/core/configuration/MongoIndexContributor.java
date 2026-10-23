package rs.teslaris.core.configuration;

import java.util.List;
import org.springframework.data.mongodb.core.index.IndexDefinition;

public interface MongoIndexContributor {

    List<MongoIndex> indexes();

    record MongoIndex(Class<?> documentType, IndexDefinition definition) {
    }
}
