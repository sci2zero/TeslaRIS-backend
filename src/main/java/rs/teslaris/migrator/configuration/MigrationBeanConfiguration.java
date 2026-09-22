package rs.teslaris.migrator.configuration;

import java.util.concurrent.Executor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableFeignClients(basePackages = "rs.teslaris.migrator.client")
public class MigrationBeanConfiguration {

    @Bean("migrationExecutor")
    public Executor migrationExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("migration-");
        executor.initialize();
        return executor;
    }
}
