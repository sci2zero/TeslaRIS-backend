package rs.teslaris.core.assessment.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.util.Pair;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResearcherAssessmentResponseDTO {

    private List<MultilingualContentDTO> commissionDescription;

    private Integer commissionId;

    private Map<String, List<Pair<String, Double>>> publicationsPerCategory = new HashMap<>();
}
