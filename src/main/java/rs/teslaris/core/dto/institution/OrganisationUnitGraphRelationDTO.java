package rs.teslaris.core.dto.institution;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitGraphRelationDTO {

    private Integer source;

    private Integer target;

    private OrganisationUnitRelationType label;
}
