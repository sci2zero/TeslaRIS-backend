package rs.teslaris.core.dto.commontypes;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResearchAreaHierarchyDTO {

    private Integer id;

    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> description;

    private ResearchAreaHierarchyDTO superResearchArea;


    public ResearchAreaHierarchyDTO(ResearchAreaHierarchyDTO other) {
        this.id = other.id;
        this.name = Objects.nonNull(other.name)
            ? other.name.stream().map(MultilingualContentDTO::new).collect(Collectors.toList())
            : null;
        this.description = Objects.nonNull(other.description)
            ?
            other.description.stream().map(MultilingualContentDTO::new).collect(Collectors.toList())
            : null;
        this.superResearchArea = Objects.nonNull(other.superResearchArea)
            ? new ResearchAreaHierarchyDTO(other.superResearchArea)
            : null;
    }
}
