package rs.teslaris.core.util.restoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Marks the current thread as performing a restoration of an entity to an earlier state, and
 * collects the references that had to be given up along the way.
 * <p>
 * Restoration is the only situation where services are allowed to silently ignore a missing
 * reference: the historical payload was valid when it was captured, and refusing to restore it
 * because an unrelated entity has since been deleted would make history unusable. Outside an open
 * context every lookup behaves exactly as before.
 * <p>
 * Caveat: the context is thread-bound. It covers the synchronous call tree of a restore, which is
 * how restores run currently, but it does not propagate into parallel streams or asynchronous work.
 */
public final class RestorationContext {

    private static final ThreadLocal<List<DegradedReference>> DEGRADED_REFERENCES =
        new ThreadLocal<>();


    private RestorationContext() {
    }

    public static List<DegradedReference> collectDuring(Supplier<Void> restoration) {
        if (isActive()) {
            throw new IllegalStateException("Restoration context is already open on this thread.");
        }

        DEGRADED_REFERENCES.set(new ArrayList<>());

        try {
            restoration.get();
            return List.copyOf(DEGRADED_REFERENCES.get());
        } finally {
            DEGRADED_REFERENCES.remove();
        }
    }

    public static boolean isActive() {
        return Objects.nonNull(DEGRADED_REFERENCES.get());
    }

    public static void report(String messageKey, String fieldPath, DegradationOutcome outcome,
                              List<String> parameters) {
        var sink = DEGRADED_REFERENCES.get();

        if (Objects.isNull(sink)) {
            return;
        }

        sink.add(DegradedReference.builder()
            .messageKey(messageKey)
            .fieldPath(fieldPath)
            .outcome(outcome)
            .parameters(Objects.requireNonNullElseGet(parameters, ArrayList::new))
            .build());
    }
}
