package rs.teslaris.core.configuration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.teslaris.core.exception.NotFoundException;

@Configuration
public class BeanConfiguration {

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
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();
    }

}
