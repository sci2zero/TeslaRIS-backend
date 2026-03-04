package rs.teslaris.importer.configuration;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ImportBeanConfiguration {

    @Bean("metadataFetchExecutor")
    public Executor metadataFetchExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("metadata-fetch-");
        executor.initialize();
        return executor;
    }
}
