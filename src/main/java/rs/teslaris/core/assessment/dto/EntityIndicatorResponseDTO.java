package rs.teslaris.core.assessment.dto;

import java.time.LocalDate;

public record EntityIndicatorResponseDTO(
    Double numericValue,

    Boolean booleanValue,

    String textualValue,

    LocalDate fromDate,

    LocalDate toDate,

    IndicatorResponseDTO indicatorResponseDTO
) {
}
