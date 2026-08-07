package rs.teslaris.core.util.restoration;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import rs.teslaris.core.service.interfaces.CRUDService;

/**
 * Single place where a missing reference is allowed to degrade instead of failing.
 * Services call these helpers at reference-resolution points instead of calling
 * {@code someService.findOne(id)} directly.
 * <p>
 * Outside a restoration the behaviour is identical to the direct call.
 */
@Slf4j
public class RestorationSupport {

    private RestorationSupport() {
    }

    public static <T> T resolveOptional(Integer id, CRUDService<T> service, String fieldPath,
                                        String messageKey) {
        return resolve(id, service, fieldPath, messageKey, DegradationOutcome.DROPPED,
            List.of(String.valueOf(id)));
    }

    /**
     * Resolves a reference that has a weaker representation to fall back on, e.g. a contributor
     * kept as a plain display name.
     */
    public static <T> T resolveDegradable(Integer id, CRUDService<T> service, String fieldPath,
                                          String messageKey, List<String> parameters) {
        return resolve(id, service, fieldPath, messageKey, DegradationOutcome.DEGRADED, parameters);
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

    private static <T> T resolve(Integer id, CRUDService<T> service, String fieldPath,
                                 String messageKey, DegradationOutcome outcome,
                                 List<String> parameters) {
        if (Objects.isNull(id)) {
            return null;
        }

        if (RestorationContext.isActive() && !service.exists(id)) {
            RestorationContext.report(messageKey, fieldPath, outcome, parameters);

            log.info("Restoration: {} missing reference '{}' (ID={}).",
                outcome.name().toLowerCase(), fieldPath, id);

            return null;
        }

        return service.findOne(id);
    }
}
