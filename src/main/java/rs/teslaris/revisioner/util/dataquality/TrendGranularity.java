package rs.teslaris.revisioner.util.dataquality;

import java.util.Objects;

public enum TrendGranularity {
    DAILY(7),
    WEEKLY(5),
    MONTHLY(6);

    public static final int MAX_POINTS = 10;

    private static final int MIN_POINTS = 2;

    private final int defaultPoints;


    TrendGranularity(int defaultPoints) {
        this.defaultPoints = defaultPoints;
    }

    public int resolvePoints(Integer requestedPoints) {
        if (Objects.isNull(requestedPoints)) {
            return defaultPoints;
        }

        return Math.min(MAX_POINTS, Math.max(MIN_POINTS, requestedPoints));
    }
}
