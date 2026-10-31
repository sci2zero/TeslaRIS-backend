package rs.teslaris.core.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CachingConfiguration {

    public static final String LANGUAGE_TAGS_CACHE = "languageTags";

    public static final String OU_HIERARCHY_CACHE = "organisationUnitHierarchy";

    public static final String GLOBAL_LEADERBOARDS_CACHE = "globalLeaderboards";

    // Global leaderboards are unparameterised, so this cache holds one entry per endpoint.
    private static final int GLOBAL_LEADERBOARDS_MAX_ENTRIES = 16;

    @Value("${reporting.leaderboards.cache-ttl-minutes:30}")
    private long leaderboardCacheTtlMinutes;


    @Bean
    public CacheManager cacheManager() {
        var cacheManager = new SimpleCacheManager();
        var leaderboardTtl = Duration.ofMinutes(leaderboardCacheTtlMinutes);

        cacheManager.setCaches(List.of(
            // Language tags are evicted explicitly on write so this cache stays unbounded.
            new CaffeineCache(LANGUAGE_TAGS_CACHE, Caffeine.newBuilder().build()),
            new CaffeineCache(OU_HIERARCHY_CACHE, Caffeine.newBuilder().build()),
            new CaffeineCache(GLOBAL_LEADERBOARDS_CACHE, Caffeine.newBuilder()
                .maximumSize(GLOBAL_LEADERBOARDS_MAX_ENTRIES)
                .expireAfterWrite(leaderboardTtl)
                .build())
        ));

        return cacheManager;
    }
}
