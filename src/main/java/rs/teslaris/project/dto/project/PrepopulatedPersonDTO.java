package rs.teslaris.project.dto.project;

import java.util.ArrayList;
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
public class PrepopulatedPersonDTO {

    private Integer personId;

    private String givenName;

    private String familyName;

    private String orcid;

    private List<MultilingualContentDTO> affiliationName = new ArrayList<>();

    private String affiliationRor;
}