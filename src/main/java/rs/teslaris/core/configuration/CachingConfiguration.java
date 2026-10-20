package rs.teslaris.core.configuration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CachingConfiguration {

    public static final String LANGUAGE_TAGS_CACHE = "languageTags";

    public static final String OU_HIERARCHY_CACHE = "organisationUnitHierarchy";


    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(LANGUAGE_TAGS_CACHE, OU_HIERARCHY_CACHE);
    }
}
