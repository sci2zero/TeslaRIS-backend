package rs.teslaris.core.util;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.model.commontypes.RecurrenceType;

/** UTC execution times with calendar recurrence in the scheduling user's timezone. */
public final class SchedulingTime {

    public static final String TIMEZONE_KEY = "schedulingTimezone";

    private static final ThreadLocal<RestorationContext> RESTORATION_ZONE = new ThreadLocal<>();

    private SchedulingTime() {
    }

    public static ZoneId requestZone() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            var zone = attrs.getRequest().getParameter("timezone");
            if (zone != null && !zone.isBlank()) {
                return ZoneId.of(zone);
            }
        }
        return RESTORATION_ZONE.get() == null ? ZoneId.systemDefault() : RESTORATION_ZONE.get().zone();
    }

    public static void restoreInZone(ZoneId zone, Instant executionTime, Runnable restore) {
        var previous = RESTORATION_ZONE.get();
        RESTORATION_ZONE.set(new RestorationContext(zone, executionTime));
        try {
            restore.run();
        } finally {
            if (previous == null) {
                RESTORATION_ZONE.remove();
            } else {
                RESTORATION_ZONE.set(previous);
            }
        }
    }

    public static Instant restoredExecutionTime() {
        var context = RESTORATION_ZONE.get();
        return context != null && context.executionTime().isAfter(Instant.now()) ?
            context.executionTime() : null;
    }

    private record RestorationContext(ZoneId zone, Instant executionTime) {
    }

    public static ZoneId zoneFromMetadata(Map<String, Object> metadata) {
        var zone = metadata.get(TIMEZONE_KEY);
        return zone == null ? ZoneId.systemDefault() : ZoneId.of(zone.toString());
    }

    public static Instant nextExecution(Instant executionTime, RecurrenceType recurrence,
                                        ZoneId zone) {
        var localTime = executionTime.atZone(zone);
        var next = switch (recurrence) {
            case DAILY -> localTime.plusDays(1);
            case WEEKLY -> localTime.plusWeeks(1);
            case MONTHLY -> localTime.plusMonths(1);
            case THREE_MONTHLY -> localTime.plusMonths(3);
            case YEARLY -> localTime.plusYears(1);
            default -> null;
        };
        return next == null ? null : next.toInstant();
    }
}
