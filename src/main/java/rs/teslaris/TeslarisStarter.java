package rs.teslaris;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableAutoConfiguration
@EnableWebSecurity
@EnableScheduling
@EnableAsync
@EnableRetry
public class TeslarisStarter {

    public static void main(String[] args) {
        // The default handler writes an Error that stops a thread only to the standard error
        // stream, which the log file does not keep.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
            LoggerFactory.getLogger(TeslarisStarter.class)
                .error("Uncaught {} killed thread '{}'", throwable.getClass().getName(),
                    thread.getName(), throwable));

        SpringApplication.run(TeslarisStarter.class, args);
    }

}
