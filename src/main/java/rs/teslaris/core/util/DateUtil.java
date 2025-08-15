package rs.teslaris.core.util;

import java.time.LocalDate;

public class DateUtil {


    public static Integer calculateYearFromProvidedValue(Integer value) {
        if (value > 0) {
            return value;
        }

        var currentYear = LocalDate.now().getYear();
        return value == 0 ? currentYear : currentYear + value;
    }
}
