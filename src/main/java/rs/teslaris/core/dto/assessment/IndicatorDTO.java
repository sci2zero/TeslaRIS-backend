package rs.teslaris.core.dto.assessment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record IndicatorDTO(

    Integer id,

    @NotBlank(message = "You have to provide indicator code.")
    String code,

    @Valid
    @NotNull(message = "You have to provide indicator title.")
    List<MultilingualContentDTO> title,

    @Valid
    List<MultilingualContentDTO> description
) {
}
