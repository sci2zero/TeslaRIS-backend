package rs.teslaris.core.configuration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.language.detect.LanguageDetector;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.TransliterationMessageSource;

@Configuration
@Slf4j
public class BeanConfiguration {

    @Value("${frontend.application.address}")
    private String frontendUrl;

    @Value("${internationalization.message.location}")
    private String messageSourceBasePackage;

    @Value("${client.localization.languages}")
    private String[] clientLocalizationLanguages;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public LanguageDetector languageDetector() {
        LanguageDetector languageDetector;
        try {
            languageDetector = LanguageDetector.getDefaultLanguageDetector().loadModels();
        } catch (IOException e) {
            throw new NotFoundException("Error while loading language models.");
        }
        return languageDetector;
    }

    @Bean
    public Cache<String, Byte> idempotencyCacheStore() {
        return CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .build();
    }

    @Bean
    public Cache<String, Byte> passwordResetRequestCacheStore() {
        return CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowCredentials(true)
                    .allowedOriginPatterns("sameOrigin")
                    .allowedOrigins(frontendUrl)
                    .allowedMethods("OPTIONS", "GET", "POST", "PUT", "DELETE", "PATCH")
                    .exposedHeaders("Link");
            }
        };
    }

    @Bean
    @Primary
    public MessageSource messageSource() {
        var messageSource = new ReloadableResourceBundleMessageSource();

        String baseName = resolveValidMessageSourceBaseName();
        messageSource.setBasenames(baseName, "classpath:internationalization/messages");
        messageSource.setCacheSeconds(60 * 5);
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(true);

        return new TransliterationMessageSource(messageSource);
    }

    private String resolveValidMessageSourceBaseName() {
        var fallback = "classpath:internationalization/messages";

        if (Objects.isNull(messageSourceBasePackage) || messageSourceBasePackage.isBlank()) {
            log.warn("No messageSourceBasePackage path configured. Falling back to classpath.");
            return fallback;
        }

        for (String lang : clientLocalizationLanguages) {
            var path = messageSourceBasePackage + "_" + lang + ".properties";
            if (!new FileSystemResource(path).exists()) {
                log.warn(
                    "Missing resource bundle for language '{}': '{}'. Falling back to classpath.",
                    lang, path);
                return fallback;
            }
        }

        log.info("Using messageSourceBasePackage at '{}'.", messageSourceBasePackage);
        return "file:" + messageSourceBasePackage;
    }

    @Bean(name = "taskExecutor")
    public Executor defaultTaskExecutor() {
        var threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("Async-");
        threadPoolTaskExecutor.setCorePoolSize(3);
        threadPoolTaskExecutor.setMaxPoolSize(3);
        threadPoolTaskExecutor.setQueueCapacity(600);
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "reindexExecutor")
    public Executor reindexExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ReindexThread-");
        executor.initialize();
        return executor;
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("TaskScheduler-");
        return scheduler;
    }

    @Bean
    public TomcatServletWebServerFactory tomcatServletWebServerFactory() {
        var factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(
            connector -> connector.setAsyncTimeout(60 * 60000)); // 1h should be enough
        return factory;
    }
}
