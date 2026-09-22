package rs.teslaris.core.dto.person;

import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpertiseOrSkillDTO {

    private Integer id;

    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> description;

    private List<MultilingualContentDTO> keywords;

    @NotNull(message = "You have to provide research area IDs.")
    private Set<Integer> researchAreasId = new HashSet<>();

    private Boolean favorite;
}
