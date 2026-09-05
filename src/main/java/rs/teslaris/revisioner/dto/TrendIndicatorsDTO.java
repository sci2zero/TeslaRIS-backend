package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;

public record TrendIndicatorsDTO(

    @Nullable
    Double current,

    @Nullable
    Double previous,

    @Nullable
    Double change,

    @Nullable
    Double best,

    @Nullable
    Double lowest
) {
    public static TrendIndicatorsDTO empty() {
        return new TrendIndicatorsDTO(null, null, null, null, null);
    }
}
