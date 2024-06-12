package rs.teslaris.core.dto.person.involvement;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EducationDTO extends InvolvementDTO {

    @Valid
    private List<MultilingualContentDTO> thesisTitle;

    @Valid
    private List<MultilingualContentDTO> title;

    @Valid
    private List<MultilingualContentDTO> abbreviationTitle;
}
