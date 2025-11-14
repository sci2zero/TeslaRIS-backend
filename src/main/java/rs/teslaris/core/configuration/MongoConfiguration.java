package rs.teslaris.core.configuration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
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
        var isDeployedLocally = host.contains("localhost") || host.contains("127.0.0.1");

        var connectionString = new ConnectionString(host + getDatabaseName());
        var mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .applyToSocketSettings(builder ->
                builder.connectTimeout(isDeployedLocally ? 10 : 20,
                        TimeUnit.SECONDS)  // TCP connect
                    .readTimeout(30, TimeUnit.SECONDS))   // Query execution
            .applyToClusterSettings(builder ->
                builder.serverSelectionTimeout(30,
                    TimeUnit.SECONDS)) // Choosing node, if deployed in cloud
            .applyToConnectionPoolSettings(pool -> pool
                .maxSize(50)
                .minSize(5)
                .maxWaitTime(10, TimeUnit.SECONDS)
            )
            .build();

        return MongoClients.create(mongoClientSettings);
    }

    @NotNull
    @Override
    public Collection<String> getMappingBasePackages() {
        return Collections.singleton("rs.teslaris.core.harvester");
    }
}
