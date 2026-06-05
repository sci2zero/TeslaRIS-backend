package rs.teslaris.assessment.dto;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.assessment.util.ClassificationPriorityMapping;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.util.functional.Triple;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResearcherAssessmentResponseDTO {

    private List<MultilingualContentDTO> commissionDescription;

    private Integer commissionId;

    private Map<String, List<Triple<String, Double, Integer>>> publicationsPerCategory =
        new TreeMap<>(ClassificationPriorityMapping.getClassificationCodeSorter());
}
