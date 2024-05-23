package rs.teslaris.core.dto.person.involvement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "You must provide a valid employment position.")
    private EmploymentPosition employmentPosition;

    @Valid
    private List<MultilingualContentDTO> role;
}
