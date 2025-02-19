package rs.teslaris.core.assessment.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.assessment.util.ClassificationPriorityMapping;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.util.Triple;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResearcherAssessmentResponseDTO {

    private List<MultilingualContentDTO> commissionDescription;

    private Integer commissionId;

    private Map<String, List<Triple<String, Double, Integer>>> publicationsPerCategory =
        new TreeMap<>(Comparator.comparingInt(ClassificationPriorityMapping::getSciListPriority));
}
