package rs.teslaris.core.model.commontypes;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class FlexibleDate {

    @Column(name = "year")
    private Integer year;

    @Column(name = "month")
    private Integer month;

    @Column(name = "day")
    private Integer day;

    @Column(name = "text")
    private String text;


    public FlexibleDate(Integer year) {
        this.year = year;
    }

    public FlexibleDate(Integer year, Integer month) {
        this.year = year;
        this.month = month;
    }

    public FlexibleDate(Integer year, Integer month, Integer day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public FlexibleDate(LocalDate localDate) {
        this.year = localDate.getYear();
        this.month = localDate.getMonthValue();
        this.day = localDate.getDayOfMonth();
    }

    public static boolean isDatePresentAndValid(FlexibleDate flexibleDate) {
        return Objects.nonNull(flexibleDate) && Objects.nonNull(flexibleDate.getYear()) &&
            flexibleDate.getYear() > 0;
    }

    /**
     * Whether a month or day is present but outside the range it could ever hold, or a day stands
     * without the month it belongs to. Imports and migrations store a zero for "unknown" rather
     * than leaving the column null, which is not a value any formatter can render.
     */
    public static boolean hasMalformedComponents(FlexibleDate flexibleDate) {
        if (Objects.isNull(flexibleDate)) {
            return false;
        }

        if (Objects.nonNull(flexibleDate.getDay()) && Objects.isNull(flexibleDate.getMonth())) {
            return true;
        }

        return !isInRange(flexibleDate.getMonth(), 12) || !isInRange(flexibleDate.getDay(), 31);
    }

    private static boolean isInRange(Integer component, int maximum) {
        return Objects.isNull(component) || (component >= 1 && component <= maximum);
    }

    @Nullable
    public static String toISOString(FlexibleDate flexibleDate) {
        if (Objects.isNull(flexibleDate) || Objects.isNull(flexibleDate.getYear())) {
            return null;
        }

        // An absent or out-of-range component truncates the string rather than being rendered. A
        // zero month would make "2011-00" and an absent one "2011-null", and neither is parseable
        // or valid ISO 8601 for the exporters.
        if (Objects.isNull(flexibleDate.getMonth()) || !isInRange(flexibleDate.getMonth(), 12)) {
            return String.format("%04d", flexibleDate.getYear());
        }

        if (Objects.isNull(flexibleDate.getDay()) || !isInRange(flexibleDate.getDay(), 31)) {
            return String.format("%04d-%02d", flexibleDate.getYear(), flexibleDate.getMonth());
        }

        return String.format("%04d-%02d-%02d",
            flexibleDate.getYear(),
            flexibleDate.getMonth(),
            flexibleDate.getDay()
        );
    }

    public static Integer getYearNumber(FlexibleDate flexibleDate) {
        if (Objects.isNull(flexibleDate) || Objects.isNull(flexibleDate.getYear()) ||
            flexibleDate.getYear() <= 0) {
            return -1;
        }

        return flexibleDate.getYear();
    }

    public static FlexibleDate now() {
        var today = LocalDate.now();
        return new FlexibleDate(today.getYear(), today.getMonthValue(), today.getDayOfMonth());
    }

    // A partial date resolves to the start of the period that it names.
    @Nullable
    public static LocalDate toLocalDate(FlexibleDate flexibleDate) {
        if (!isDatePresentAndValid(flexibleDate) || hasMalformedComponents(flexibleDate)) {
            return null;
        }

        try {
            return LocalDate.of(
                flexibleDate.getYear(),
                Objects.requireNonNullElse(flexibleDate.getMonth(), 1),
                Objects.requireNonNullElse(flexibleDate.getDay(), 1)
            );
        } catch (DateTimeException e) {
            // A day the month does not have, such as 30 February.
            return null;
        }
    }
}
