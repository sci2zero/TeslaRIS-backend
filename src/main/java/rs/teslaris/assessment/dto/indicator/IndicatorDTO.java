package rs.teslaris.assessment.dto.indicator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
import rs.teslaris.assessment.model.indicator.IndicatorContentType;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;

public record IndicatorDTO(

    Integer id,

    @NotBlank(message = "You have to provide indicator code.")
    String code,

    @Valid
    @NotNull(message = "You have to provide indicator title.")
    List<MultilingualContentDTO> title,

    @Valid
    List<MultilingualContentDTO> description,

    @NotNull(message = "You have to provide an access level for indicator")
    AccessLevel indicatorAccessLevel,

    @NotNull(message = "You have to provide applicable entity types for indicator")
    List<ApplicableEntityType> applicableTypes,

    @NotNull(message = "You have to provide content type for indicator")
    IndicatorContentType contentType
) {
}
