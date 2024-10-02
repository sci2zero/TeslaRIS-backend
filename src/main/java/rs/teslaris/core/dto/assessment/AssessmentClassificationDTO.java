package rs.teslaris.core.dto.assessment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record AssessmentClassificationDTO(

    Integer id,

    @NotBlank(message = "You have to provide formal rule description.")
    String formalDescriptionOfRule,

    @NotBlank(message = "You have to provide assessment classification code.")
    String code,

    @Valid
    @NotNull(message = "You have to provide assessment classification title.")
    List<MultilingualContentDTO> title
) {
}
