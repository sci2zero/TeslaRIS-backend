package rs.teslaris.core.dto.institution;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitDeleteRequest {

    @NotNull(message = "You have to provide a list of IDs.")
    private List<Integer> organisationUnitIds;
}
