package rs.teslaris.reporting.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import rs.teslaris.core.applicationevent.PublicVisibilityChangedEvent;
import rs.teslaris.core.configuration.CachingConfiguration;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.reporting.service.impl.leaderboards.GlobalLeaderboardCacheEvictionListener;

public class GlobalLeaderboardCacheEvictionListenerTest {

    @Test
    void shouldClearGlobalLeaderboardsWhenVisibilityChanges() {
        // Given
        var cacheManager = mock(CacheManager.class);
        var cache = mock(Cache.class);
        when(cacheManager.getCache(CachingConfiguration.GLOBAL_LEADERBOARDS_CACHE))
            .thenReturn(cache);
        var listener = new GlobalLeaderboardCacheEvictionListener(cacheManager);

        // When
        listener.handlePublicVisibilityChanged(
            new PublicVisibilityChangedEvent(EntityType.PERSON.name(), 1, false));

        // Then
        verify(cache).clear();
    }

    @Test
    void shouldIgnoreMissingCache() {
        // Given
        var listener = new GlobalLeaderboardCacheEvictionListener(mock(CacheManager.class));

        // When / Then
        assertDoesNotThrow(() -> listener.handlePublicVisibilityChanged(
            new PublicVisibilityChangedEvent(EntityType.PERSON.name(), 1, false)));
    }
}
