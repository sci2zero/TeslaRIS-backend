package rs.teslaris.core.dto.person.involvement;

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

    private List<MultilingualContentDTO> role;
}
