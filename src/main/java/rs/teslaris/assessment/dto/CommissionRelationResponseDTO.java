package rs.teslaris.assessment.dto;

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
public class CommissionRelationResponseDTO {

    private Integer id;

    private Integer sourceCommissionId;

    private List<SimpleCommissionResponseDTO> targetCommissions;

    private Integer priority;

    private ResultCalculationMethod resultCalculationMethod;
}
