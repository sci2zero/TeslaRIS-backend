package rs.teslaris.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReorderCommissionRelationDTO {

    private Integer oldRelationPriority;

    private Integer newRelationPriority;
}
