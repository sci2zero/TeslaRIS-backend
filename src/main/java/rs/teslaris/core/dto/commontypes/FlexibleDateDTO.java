package rs.teslaris.core.dto.commontypes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FlexibleDateDTO(
    @NotNull(message = "Year must be present.")
    @Positive(message = "Year must be a positive number")
    Integer year,

    @Positive(message = "Month must be a positive number")
    Integer month,

    @Positive(message = "Day must be a positive number")
    Integer day,
    String text
) {

    public FlexibleDateDTO(@NotNull(message = "Year must be present.")
                           @Positive(message = "Year must be a positive number")
                           Integer year, @Positive(message = "Month must be a positive number")
                           Integer month, @Positive(message = "Day must be a positive number")
                           Integer day, String text) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.text = text;
    }
}
