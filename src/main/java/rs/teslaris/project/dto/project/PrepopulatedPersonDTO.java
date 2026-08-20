package rs.teslaris.project.dto.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepopulatedPersonDTO {

    // TODO: Add MLC
    private String givenName;

    private String familyName;

    private String orcid;

    private String affiliationName;

    private String affiliationRor;
}