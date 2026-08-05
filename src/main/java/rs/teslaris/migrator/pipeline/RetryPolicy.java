package rs.teslaris.migrator.pipeline;

import java.time.Duration;

public record RetryPolicy(
    int maxAttempts,
    Duration initialBackoff,
    double multiplier
) {

    public static RetryPolicy none() {
        return new RetryPolicy(1, Duration.ZERO, 1.0);
    }

    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, Duration.ofSeconds(2), 2.0);
    }

    public Duration backoffFor(int attempt) {
        if (attempt <= 1) {
            return initialBackoff;
        }

        return Duration.ofMillis(
            (long) (initialBackoff.toMillis() * Math.pow(multiplier, attempt - 1.0)));
    }
}
