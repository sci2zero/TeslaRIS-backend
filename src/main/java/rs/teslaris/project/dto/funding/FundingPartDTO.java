package rs.teslaris.project.dto.funding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FundingPartDTO {

    private Integer id;

    @NotNull(message = "You have to provide project ID.")
    @Positive(message = "Funding ID cannot be a negative number.")
    private Integer fundingId;

    @Valid
    @NotNull(message = "You have to provide a funding part description.")
    @NotEmpty(message = "You have to provide a funding part description.")
    private List<MultilingualContentDTO> description;

    @NotNull(message = "You have to specify amount")
    @Valid
    private MonetaryAmountDTO amount;

    private Integer projectEventId;

    private Integer projectDocumentId;

    private Integer fundingApplicationId;

    private Integer personProjectContributionId;

    private Integer organisationUnitProjectContributionId;
}
