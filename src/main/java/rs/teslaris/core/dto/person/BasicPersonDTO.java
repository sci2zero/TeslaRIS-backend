package rs.teslaris.core.dto.person;

import java.time.LocalDate;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
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

    @Valid
    private PersonNameDTO personName;

    @NotBlank(message = "You must provide a contact email.")
    private String contactEmail;

    @NotNull(message = "You must provide a person sex.")
    private Sex sex;

    @NotNull(message = "You must provide a birth date.")
    private LocalDate localBirthDate;

    @NotBlank(message = "You must provide a contact phone number.")
    private String phoneNumber;

    private String apvnt;

    private String mnid;

    private String orcid;

    private String scopusAuthorId;

    @Positive(message = "Organisation unit id must be a positive number.")
    @NotNull(message = "You have to provide a organisation unit ID.")
    private Integer organisationUnitId;

    @NotNull(message = "You must provide a person employment position.")
    private EmploymentPosition employmentPosition;
}
