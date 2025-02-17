package rs.teslaris.core.assessment.dto;

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
public class EnrichedResearcherAssessmentResponseDTO extends ResearcherAssessmentResponseDTO {

    private Integer fromYear;

    private Integer toYear;

    private String personName;

    private EmploymentPosition personPosition;

    private List<MultilingualContentDTO> institutionName;
}
