package rs.teslaris.migrator.configuration;

import java.util.concurrent.Executor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

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

        // Runs act as the admin who started them: core services read the logged-in user (e.g.
        // document creation skips contributor notifications for trusted roles) and fail without it.
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
