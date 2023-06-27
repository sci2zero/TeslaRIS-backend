package rs.teslaris.core.dto.institution;

import java.util.List;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResearchAreaDTO {

    private Integer id;

    @Valid
    private List<MultilingualContentDTO> name;

    @Valid
    private List<MultilingualContentDTO> description;

    private Integer superResearchAreaId;
}
