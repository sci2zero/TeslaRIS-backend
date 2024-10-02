package rs.teslaris.core.dto.assessment;

import java.time.LocalDate;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;

public record AssessmentRulebookResponseDTO(
    Integer id,

    List<MultilingualContentDTO> title,

    List<MultilingualContentDTO> description,

    LocalDate issueDate,

    DocumentFileResponseDTO pdfFile,

    Integer publisherId,

    Integer assessmentMeasureId
) {
}
