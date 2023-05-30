package rs.teslaris.core.dto.institution;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ResearchAreaDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitDTORequest {
    private List<MultilingualContentDTO> name;

    private String nameAbbreviation;

    private List<MultilingualContentDTO> keyword;

    private List<Integer> researchAreasId;

    private GeoLocationDTO location;

    private ApproveStatus approveStatus;

    private ContactDTO contact;
}
