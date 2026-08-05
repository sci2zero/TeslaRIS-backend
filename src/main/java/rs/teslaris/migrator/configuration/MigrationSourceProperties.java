package rs.teslaris.migrator.configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.pipeline.RetryPolicy;

@Component
@ConfigurationProperties(prefix = "migrator")
@Getter
@Setter
public class MigrationSourceProperties {

    private int defaultBatchSize = 200;

    private Map<String, SourceProperties> sources = new HashMap<>();


    public SourceProperties forSource(String name) {
        return sources.getOrDefault(name, new SourceProperties());
    }

    @Getter
    @Setter
    public static class SourceProperties {

        private String baseUrl;

        private String apiKeyHeader = "X-API-KEY";

        private String apiKey;

        private Integer batchSize;

        private RetryProperties retry = new RetryProperties();

        public int batchSizeOrDefault(int fallback) {
            return batchSize == null ? fallback : batchSize;
        }
    }

    @Getter
    @Setter
    public static class RetryProperties {

        private int maxAttempts = 3;

        private Duration initialBackoff = Duration.ofSeconds(2);

        private double multiplier = 2.0;

        public RetryPolicy toPolicy() {
            return new RetryPolicy(maxAttempts, initialBackoff, multiplier);
        }
    }
}
