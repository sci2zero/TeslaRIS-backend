package rs.teslaris.core.dto.document;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonContributionDTO {

    @NotNull(message = "You have to provide a person ID.")
    @Positive(message = "Person ID must be a positive number.")
    Integer personId;

    @Valid
    List<MultilingualContentDTO> contributionDescription;

    @NotNull(message = "You have to specify an order number.")
    @Positive(message = "Order number must be a positive number.")
    Integer orderNumber;

    @NotNull(message = "You have to provide a list of institution Ids.")
    List<Integer> institutionIds;
}
