package rs.teslaris.core.dto.person.involvement;

import jakarta.validation.Valid;
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
public class MembershipDTO extends InvolvementDTO {

    @Valid
    private List<MultilingualContentDTO> contributionDescription;

    @Valid
    private List<MultilingualContentDTO> role;

}
