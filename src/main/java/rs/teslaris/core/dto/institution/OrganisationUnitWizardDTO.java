package rs.teslaris.core.dto.institution;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitWizardDTO extends OrganisationUnitDTORequest {

    private List<MultilingualContentDTO> superOrganisationUnitName;

    private Integer superOrganisationUnitId;
}
