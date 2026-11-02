package rs.teslaris.project.dto.identifier;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.identifier.EntityIdentifierDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FundingIdentifierDTO extends EntityIdentifierDTO {

    @NotNull(message = "You have to provide funding ID.")
    @Positive(message = "Funding ID must be a positive number.")
    private Integer fundingId;


    public FundingIdentifierDTO(String value, Integer identifierId, Integer fundingId) {
        super(value, identifierId);
        this.fundingId = fundingId;
    }
}
