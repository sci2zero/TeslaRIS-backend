package rs.teslaris.core.importer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonLoadDTO {

    private String firstName;

    private String middleName;

    private String lastName;

    private String apvnt;

    private String mnid;

    private String orcid;

    private String scopusAuthorId;
}
