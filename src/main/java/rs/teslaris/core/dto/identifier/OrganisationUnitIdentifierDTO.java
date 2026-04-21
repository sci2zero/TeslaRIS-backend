package rs.teslaris.core.dto.identifier;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitIdentifierDTO extends EntityIdentifierDTO {

    @NotNull(message = "You have to provide organisation unit ID.")
    @Positive(message = "Organisation unit ID must be a positive number.")
    private Integer organisationUnitId;
}
