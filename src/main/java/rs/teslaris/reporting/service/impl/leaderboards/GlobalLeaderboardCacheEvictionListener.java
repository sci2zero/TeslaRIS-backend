package rs.teslaris.reporting.service.impl.leaderboards;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.teslaris.core.applicationevent.PublicVisibilityChangedEvent;
import rs.teslaris.core.configuration.CachingConfiguration;

@Component
public class GlobalLeaderboardCacheEvictionListener {

    private final CacheManager cacheManager;


    @Autowired
    public GlobalLeaderboardCacheEvictionListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handlePublicVisibilityChanged(PublicVisibilityChangedEvent event) {
        var cache = cacheManager.getCache(CachingConfiguration.GLOBAL_LEADERBOARDS_CACHE);

        if (Objects.nonNull(cache)) {
            cache.clear();
        }
    }
}
