package rs.teslaris.core.util.restoration;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rs.teslaris.core.service.interfaces.ExistenceCheckable;
import rs.teslaris.core.util.exceptionhandling.exception.RevisionRestoreException;

/**
 * Single place where a missing reference is allowed to degrade instead of failing.
 * Services call these helpers at reference-resolution points instead of calling
 * {@code someService.findOne(id)} directly.
 * <p>
 * Outside a restoration the behaviour is identical to the direct call.
 */
@NoArgsConstructor
@Slf4j
public class RestorationSupport {

    public static <T> T resolveOptional(Integer id, ExistenceCheckable service,
                                        Function<Integer, T> lookup, String fieldPath,
                                        String messageKey) {
        return resolve(id, service, lookup, fieldPath, messageKey, DegradationOutcome.DROPPED,
            List.of(String.valueOf(id)));
    }

    /**
     * Resolves a reference that has a weaker representation to fall back on, e.g. a contributor
     * kept as a plain display name.
     */
    public static <T> T resolveDegradable(Integer id, ExistenceCheckable service,
                                          Function<Integer, T> lookup, String fieldPath,
                                          String messageKey, List<String> parameters) {
        return resolve(id, service, lookup, fieldPath, messageKey, DegradationOutcome.DEGRADED,
            parameters);
    }

    /**
     * Resolves a reference the entity cannot exist without, e.g. the journal of a journal
     * publication. Restoring into an orphaned state is worse than refusing, so a restoration fails
     * here, but it fails cleanly, before a transactional {@code findOne} can mark the transaction
     * rollback-only and turn the failure into an opaque commit error.
     */
    public static void requireExists(Integer id, ExistenceCheckable service,
                                     String fieldPath) {
        if (RestorationContext.isActive() && Objects.nonNull(id) && !service.exists(id)) {
            throw new RevisionRestoreException(
                String.format("cantRestoreVersionMessage:%s:%d", fieldPath, id)
            );
        }
    }

    /**
     * Reports references that a bulk lookup silently did not return.
     */
    public static void reportMissingFromBulkLookup(Collection<Integer> requestedIds,
                                                   Collection<Integer> resolvedIds,
                                                   String fieldPath, String messageKey) {
        if (!RestorationContext.isActive() || Objects.isNull(requestedIds)) {
            return;
        }

        requestedIds.stream()
            .filter(Objects::nonNull)
            .filter(id -> !resolvedIds.contains(id))
            .forEach(id -> {
                RestorationContext.report(messageKey, fieldPath, DegradationOutcome.DROPPED,
                    List.of(String.valueOf(id)));

                log.info("Restoration: dropped missing reference '{}' (ID={}).", fieldPath, id);
            });
    }

    private static <T> T resolve(Integer id, ExistenceCheckable service,
                                 Function<Integer, T> lookup, String fieldPath, String messageKey,
                                 DegradationOutcome outcome, List<String> parameters) {
        if (Objects.isNull(id)) {
            return null;
        }

        if (RestorationContext.isActive() && !service.exists(id)) {
            RestorationContext.report(messageKey, fieldPath, outcome, parameters);

            log.info("Restoration: {} missing reference '{}' (ID={}).",
                outcome.name().toLowerCase(), fieldPath, id);

            return null;
        }

        return lookup.apply(id);
    }
}
