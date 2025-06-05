package rs.teslaris.assessment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.institution.ResultCalculationMethod;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommissionRelationDTO {

    @NotNull(message = "You have to specify source commission ID.")
    private Integer sourceCommissionId;

    @NotNull(message = "You have to specify target commission IDs.")
    private List<Integer> targetCommissionIds;

    @NotNull(message = "You have to specify priority.")
    private Integer priority;

    @NotNull(message = "You have to specify result calculation method.")
    private ResultCalculationMethod resultCalculationMethod;
}
