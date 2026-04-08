package rs.teslaris.core.dto.person.involvement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record EmploymentPositionDTO(
    Integer id,

    @NotNull(message = "You have to provide name.")
    List<MultilingualContentDTO> name,

    @NotNull(message = "You have to provide description.")
    List<MultilingualContentDTO> description,

    String processedName,

    String schemeName,

    @Positive(message = "Super employment position ID must be a positive number.")
    Integer superEmploymentPositionId,

    List<MultilingualContentDTO> superEmploymentPositionName
) {
}
