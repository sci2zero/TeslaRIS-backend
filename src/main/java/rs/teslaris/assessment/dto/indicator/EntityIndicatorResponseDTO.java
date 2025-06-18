package rs.teslaris.assessment.dto.indicator;

import java.time.LocalDate;
import java.util.List;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;

public record EntityIndicatorResponseDTO(
    Integer id,
    Double numericValue,

    Boolean booleanValue,

    String textualValue,

    LocalDate fromDate,

    LocalDate toDate,

    IndicatorResponseDTO indicatorResponse,

    EntityIndicatorSource source,

    List<DocumentFileResponseDTO> proofs
) {
}
