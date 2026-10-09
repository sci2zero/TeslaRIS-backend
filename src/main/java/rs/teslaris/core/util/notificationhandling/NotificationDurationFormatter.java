package rs.teslaris.core.util.notificationhandling;

import java.time.Duration;
import java.util.Locale;

public final class NotificationDurationFormatter {

    private NotificationDurationFormatter() {
    }

    public static String format(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        if (duration.compareTo(Duration.ofSeconds(1)) < 0) {
            return String.format(Locale.ROOT, "%.2fs", (duration.toNanos() / 10_000_000) / 100.0);
        }
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        }
        if (seconds < 3600) {
            return seconds / 60 + "m " + seconds % 60 + "s";
        }
        return seconds / 3600 + "h " + (seconds % 3600) / 60 + "m " + seconds % 60 + "s";
    }
}
