package rs.teslaris.core.model.commontypes;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
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

    @Nullable
    public static String toISOString(FlexibleDate flexibleDate) {
        if (Objects.isNull(flexibleDate) || Objects.isNull(flexibleDate.getYear())) {
            return null;
        }

        if (Objects.isNull(flexibleDate.getMonth())) {
            return String.format("%04d", flexibleDate.getYear());
        }

        if (Objects.isNull(flexibleDate.getDay())) {
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
}
