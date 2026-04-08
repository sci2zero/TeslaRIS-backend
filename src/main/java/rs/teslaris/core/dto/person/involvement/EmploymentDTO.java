package rs.teslaris.core.dto.person.involvement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.person.EmploymentPosition;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentDTO extends InvolvementDTO {

    private EmploymentPosition employmentPosition;

    @Positive(message = "Employment position id must be a positive number.")
    private Integer employmentPositionId;

    @Valid
    private List<MultilingualContentDTO> role;

    // used only for responses

    private List<MultilingualContentDTO> employmentPositionName;
}
