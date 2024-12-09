package rs.teslaris.core.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record AssessmentMeasureDTO(

    Integer id,

    @NotBlank(message = "You have to provide formal rule description.")
    String formalDescriptionOfRule,

    @NotBlank(message = "You have to provide assessment measure code.")
    String code,

    @NotNull(message = "You have to provide the reward value.")
    Double value,

    @Valid
    @NotNull(message = "You have to provide assessment measure title.")
    List<MultilingualContentDTO> title,

    @NotNull(message = "You have to provide a rulebook ID.")
    @Positive(message = "Assessment rulebook ID must be a positive number.")
    Integer assessmentRulebookId
) {
}
