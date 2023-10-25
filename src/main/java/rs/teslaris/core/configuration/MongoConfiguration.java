package rs.teslaris.core.configuration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfiguration extends AbstractMongoClientConfiguration {

    @Value("${mongo.host}")
    private String host;

    @Value("${mongo.database}")
    private String database;


    @NotNull
    @Override
    protected String getDatabaseName() {
        return database;
    }

    @NotNull
    @Override
    public MongoClient mongoClient() {
        var connectionString = new ConnectionString(host + getDatabaseName());
        var mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .build();

        return MongoClients.create(mongoClientSettings);
    }

    @NotNull
    @Override
    public Collection getMappingBasePackages() {
        return Collections.singleton("rs.teslaris.core.harvester");
    }
}
