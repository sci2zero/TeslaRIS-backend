package rs.teslaris.core.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;

public record AssessmentRulebookDTO(

    @Valid
    @NotNull(message = "You have to provide assessment rulebook name.")
    List<MultilingualContentDTO> name,

    @Valid
    List<MultilingualContentDTO> description,

    @NotNull(message = "You have to provide an issue date.")
    LocalDate issueDate,

    @Valid
    DocumentFileDTO pdfFile,

    @Positive(message = "Publisher ID must be > 0")
    Integer publisherId,

    @NotNull(message = "You have to provide an assessment measure ID.")
    @Positive(message = "Assessment measure ID must be > 0")
    Integer assessmentMeasureId
) {
}
