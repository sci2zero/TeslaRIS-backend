package rs.teslaris.core.dto.institution;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RelationGraphDataDTO {

    private List<OrganisationUnitDTO> nodes;

    private List<OrganisationUnitGraphRelationDTO> links;
}
