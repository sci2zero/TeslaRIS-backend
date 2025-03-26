package rs.teslaris.assessment.dto;

import java.time.LocalDate;
import rs.teslaris.assessment.model.EntityIndicatorSource;

public record PublicationSeriesIndicatorResponseDTO(
    Integer id,

    Double numericValue,

    Boolean booleanValue,

    String textualValue,

    LocalDate fromDate,

    LocalDate toDate,

    IndicatorResponseDTO indicatorResponse,

    EntityIndicatorSource source,

    String categoryIdentifier
) {
}
