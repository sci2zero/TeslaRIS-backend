package rs.teslaris.core.dto.person.involvement;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ResearchAreaHierarchyDTO;
import rs.teslaris.core.model.person.DegreeType;
import rs.teslaris.core.model.person.EducationStatus;

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

    private DegreeType degreeType;

    private EducationStatus educationStatus;

    @Valid
    private List<MultilingualContentDTO> degreeCode;

    @Valid
    private List<MultilingualContentDTO> degreeClassification;

    private Set<Integer> researchAreasId = new HashSet<>();

    // used only for responses

    private List<ResearchAreaHierarchyDTO> researchAreas = new ArrayList<>();
}
