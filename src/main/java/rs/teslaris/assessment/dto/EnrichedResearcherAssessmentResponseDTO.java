package rs.teslaris.assessment.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class EnrichedResearcherAssessmentResponseDTO extends ResearcherAssessmentResponseDTO {

    private Integer fromYear;

    private Integer toYear;

    private String personName;

    private EmploymentPosition personPosition;

    private List<MultilingualContentDTO> institutionName;

    private Map<Integer, List<Integer>> publicationToInstitution =
        Collections.synchronizedMap(new HashMap<>());
}
