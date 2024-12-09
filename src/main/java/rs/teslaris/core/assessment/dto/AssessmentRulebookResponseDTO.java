package rs.teslaris.core.assessment.dto;

import java.time.LocalDate;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;

public record AssessmentRulebookResponseDTO(
    Integer id,

    List<MultilingualContentDTO> name,

    List<MultilingualContentDTO> description,

    LocalDate issueDate,

    DocumentFileResponseDTO pdfFile,

    Integer publisherId,

    List<MultilingualContentDTO> publisherName
) {
}
