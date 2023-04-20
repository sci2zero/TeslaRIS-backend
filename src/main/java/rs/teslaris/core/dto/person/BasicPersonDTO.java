package rs.teslaris.core.dto.person;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.Sex;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BasicPersonDTO {

    private Integer id;

    private PersonNameDTO personName;

    private String contactEmail;

    private Sex sex;

    private LocalDate localBirthDate;

    private String phoneNumber;

    private String apvnt;

    private String mnid;

    private String orcid;

    private String scopusAuthorId;

    private Integer organisationUnitId;

    private EmploymentPosition employmentPosition;
}
