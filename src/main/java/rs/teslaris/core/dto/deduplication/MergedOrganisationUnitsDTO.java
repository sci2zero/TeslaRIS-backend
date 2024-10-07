package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedOrganisationUnitsDTO {

    private OrganisationUnitRequestDTO leftOrganisationUnit;

    private OrganisationUnitRequestDTO rightOrganisationUnit;
}
