package rs.teslaris.core.dto.institution;


import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.ContactDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitRequestDTO {

    @NotNull(message = "You have to provide organisation name.")
    private List<MultilingualContentDTO> name;

    private String nameAbbreviation;

    @NotNull(message = "You have to provide organisation keywords.")
    private List<MultilingualContentDTO> keyword;

    @NotNull(message = "You have to provide research areas.")
    private List<Integer> researchAreasId;

    private GeoLocationDTO location;

    private ContactDTO contact;
}
