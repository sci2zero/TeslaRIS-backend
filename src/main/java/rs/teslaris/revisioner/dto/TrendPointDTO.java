package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;
import java.time.LocalDate;

public record TrendPointDTO(

    String label,

    LocalDate periodEnd,

    @Nullable
    Double value,

    long recordsAssessed
) {
}
