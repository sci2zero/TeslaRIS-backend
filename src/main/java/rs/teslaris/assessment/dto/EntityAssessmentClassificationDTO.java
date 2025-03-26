package rs.teslaris.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityAssessmentClassificationDTO {

    @NotNull(message = "You have to provide commission ID.")
    private Integer commissionId;

    @NotNull(message = "You have to provide assessment classification ID.")
    private Integer assessmentClassificationId;

    private Integer classificationYear;
}
