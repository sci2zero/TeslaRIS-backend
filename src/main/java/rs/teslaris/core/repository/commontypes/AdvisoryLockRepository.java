package rs.teslaris.core.repository.commontypes;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdvisoryLockRepository {

    public static final String DOCUMENT_VALIDATION_SCOPE = "document-validation";

    public static final String REVISION_WRITE_SCOPE_PREFIX = "revision-write:";

    private final EntityManager entityManager;


    /**
     * Waits until the lock is free. The caller holds its database connection while waiting, so this
     * is for keys which are rarely contended.
     * <p>
     * A scope and a key together identify one lock, so two call sites which guard different
     * invariants of the same entity have to use scopes of their own, or one of them waits for a
     * lock which says nothing about its own work.
     */
    public void lock(String scope, Integer key) {
        entityManager.createNativeQuery(
                "SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(:scope), "
                    + "CAST(:key AS int))) AS acquired")
            .setParameter("scope", scope)
            .setParameter("key", key)
            .getSingleResult();
    }

    /**
     * @return false when somebody else holds the lock, in which case the caller never waits.
     */
    public boolean tryLock(String scope, Integer key) {
        return (Boolean) entityManager.createNativeQuery(
                "SELECT pg_try_advisory_xact_lock(hashtext(:scope), CAST(:key AS int))")
            .setParameter("scope", scope)
            .setParameter("key", key)
            .getSingleResult();
    }
}
