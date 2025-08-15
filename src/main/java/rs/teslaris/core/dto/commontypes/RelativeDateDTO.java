package rs.teslaris.core.dto.commontypes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.jetbrains.annotations.NotNull;

public record RelativeDateDTO(
    Integer day,
    Integer month,
    Integer year
) {
    public static RelativeDateDTO parse(String source) {
        try {
            var date = LocalDate.parse(source); // additional support for yyyy-MM-dd
            return new RelativeDateDTO(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
        } catch (DateTimeParseException ignored) {
            var tokens = source.split("\\|"); // expects yyyy|MM|dd

            return new RelativeDateDTO(
                Integer.parseInt(tokens[2]),
                Integer.parseInt(tokens[1]),
                Integer.parseInt(tokens[0])
            );
        }
    }

    public static RelativeDateDTO of(Integer year, Integer month, Integer day) {
        return new RelativeDateDTO(day, month, year);
    }

    public static RelativeDateDTO now() {
        var currentDate = LocalDate.now();
        return new RelativeDateDTO(currentDate.getDayOfMonth(), currentDate.getMonthValue(),
            currentDate.getYear());
    }

    public LocalDate computeDate() {
        LocalDate date = LocalDate.now();

        if (this.day > 0) {
            date = date.withDayOfMonth(this.day);
        } else if (this.day < 0) {
            date = date.plusDays(this.day);
        }

        if (this.month > 0) {
            date = date.withMonth(this.month);
        } else if (this.month < 0) {
            date = date.plusMonths(this.month);
        }

        if (this.year > 0) {
            date = date.withYear(this.year);
        } else if (this.year < 0) {
            date = date.plusYears(this.year);
        }

        return date;
    }

    @Override
    @NotNull
    public String toString() {
        return year + "|" + month + "|" + day;
    }
}
