package rs.teslaris.core.dto.person.involvement;

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

    private List<MultilingualContentDTO> thesisTitle;

    private List<MultilingualContentDTO> title;

    private List<MultilingualContentDTO> abbreviationTitle;
}
