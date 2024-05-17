package rs.teslaris.core.dto.commontypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReorderContributionRequestDTO {

    private Integer oldContributionOrderNumber;

    private Integer newContributionOrderNumber;
}
