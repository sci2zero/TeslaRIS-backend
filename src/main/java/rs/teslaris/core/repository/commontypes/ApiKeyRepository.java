package rs.teslaris.core.repository.commontypes;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.ApiKey;
import rs.teslaris.core.model.commontypes.ApiKeyType;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {

    @Query("SELECT key FROM ApiKey key WHERE key.lookupHash = :lookupHash AND key.usageType = :type")
    List<ApiKey> findByLookupHashAndType(String lookupHash, ApiKeyType type);

    @Modifying
    void deleteByValidUntilLessThanEqual(LocalDate date);

    @Modifying
    @Query("UPDATE ApiKey key SET key.timesUsedToday = 0")
    void resetApiKeyDailyUsage();
}
