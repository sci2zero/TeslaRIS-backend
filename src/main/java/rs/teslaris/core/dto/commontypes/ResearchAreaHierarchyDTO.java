package rs.teslaris.core.dto.commontypes;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResearchAreaDTO {

    private Integer id;

    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> description;

    private ResearchAreaDTO superResearchArea;
}
