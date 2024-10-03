package rs.teslaris.core.assessment.dto;

import java.time.LocalDate;
import java.util.List;

public record EntityIndicatorResponseDTO(
    Double numericValue,

    Boolean booleanValue,

    String textualValue,

    LocalDate fromDate,

    LocalDate toDate,

    List<String> urls,

    IndicatorResponseDTO indicatorResponseDTO
) {
}
