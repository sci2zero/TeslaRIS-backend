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
        new TreeMap<>((s1, s2) -> {
            double score1 = ClassificationPriorityMapping.calculateSortingScore(s1);
            double score2 = ClassificationPriorityMapping.calculateSortingScore(s2);

            if (score1 != Double.MAX_VALUE && score2 != Double.MAX_VALUE) {
                return Double.compare(score1, score2);
            }

            if (score1 == Double.MAX_VALUE && score2 == Double.MAX_VALUE) {
                return s1.compareTo(s2);
            }

            if (score1 != Double.MAX_VALUE) {
                return -1;
            } else {
                return 1;
            }
        });
}
