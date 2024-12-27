package rs.teslaris.core.dto.institution;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
public class ResearchAreaDTO {

    private Integer id;

    @Valid
    private List<MultilingualContentDTO> name;

    @Valid
    private List<MultilingualContentDTO> description;

    @Positive(message = "Super research area ID must be a positive number.")
    private Integer superResearchAreaId;
}
