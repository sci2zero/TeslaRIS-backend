package rs.teslaris.assessment.dto.classification;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventAssessmentClassificationDTO extends EntityAssessmentClassificationDTO {

    private Integer eventId;
}
